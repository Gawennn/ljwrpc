package com.ljw.proxy.handler;

import com.ljw.LjwrpcBootstrap;
import com.ljw.NettyBootstrapInitializer;
import com.ljw.annotation.TryTimes;
import com.ljw.compress.CompressorFactory;
import com.ljw.discovery.Registry;
import com.ljw.enumeration.RequestType;
import com.ljw.exceptions.DiscoveryException;
import com.ljw.exceptions.NetworkException;
import com.ljw.protection.CircuitBreaker;
import com.ljw.serialize.SerializerFactory;
import com.ljw.transport.LjwrpcRequest;
import com.ljw.transport.RequestPayload;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 该处封装了客户端通信的基础逻辑，每一个代理对象的远程调用过程都封装在了invoke方法中
 * 1.发现可用服务
 * 2.建立连接
 * 3.发送请求
 * 4.得到结果
 *
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class RpcConsumerInvocationHandler implements InvocationHandler {

    // 此处需要一个注册中心，和一个接口
    private final Registry registry;
    private final Class<?> interfaceRef;
    private String group;

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef, String group) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
        this.group = group;
    }

    /**
     * 所有的方法调用，本质都会走到这里
     *
     * @param proxy  the proxy instance that the method was invoked on
     * @param method the {@code Method} instance corresponding to
     *               the interface method invoked on the proxy instance.  The declaring
     *               class of the {@code Method} object will be the interface that
     *               the method was declared in, which may be a superinterface of the
     *               proxy interface that the proxy class inherits the method through.
     * @param args   an array of objects containing the values of the
     *               arguments passed in the method invocation on the proxy instance,
     *               or {@code null} if interface method takes no arguments.
     *               Arguments of primitive types are wrapped in instances of the
     *               appropriate primitive wrapper class, such as
     *               {@code java.lang.Integer} or {@code java.lang.Boolean}.
     * @return 返回值
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 从接口中获取判断是否需要重试
        TryTimes tryTimesAnnotation = method.getAnnotation(TryTimes.class);

        // 默认值0，代表不重试
        int tryTimes = 0;
        int intervalTime = 0;
        if (tryTimesAnnotation != null) {
            tryTimes = tryTimesAnnotation.tryTimes();
            intervalTime = 2000;
        }

        while (true) {
            // 什么情况下需要重试 1.异常 2.响应有问题 code==500

                /*
                -----------------------封装报文----------------------
                 */
            RequestPayload requestPayload = RequestPayload.builder()
                    .interfaceName(interfaceRef.getName())
                    .methodName(method.getName())
                    .parametersType(method.getParameterTypes())
                    .parameterValue(args)
                    .returnType(method.getReturnType())
                    .build();

            // 1.创建一个请求
            LjwrpcRequest ljwrpcRequest = LjwrpcRequest.builder()
                    .requestId(LjwrpcBootstrap.getInstance().getConfiguration().getIdGenerator().getId())
                    .compressType(CompressorFactory.getCompressor(LjwrpcBootstrap.getInstance().getConfiguration().getCompressType()).getCode())
                    .requestType(RequestType.REQUEST.getId())
                    .serializeType(SerializerFactory.getSerializer(LjwrpcBootstrap.getInstance().getConfiguration().getSerializeType()).getCode())
                    .timeStamp(System.currentTimeMillis())
                    .requestPayload(requestPayload)
                    .build();

            // 2.将请求存入本地线程，需要在合适的时候remove
            LjwrpcBootstrap.REQUEST_THREAD_LOCAL.set(ljwrpcRequest);

            // 3、发现服务，从注册中心拉取服务列表，并通过客户端负载均衡寻找一个可用的服务实例
            // 传入服务的名字，返回ip+端口
            // 通过实现LoadBalancer接口，并重写selectServiceAddress方法获取负载均衡器，并再进行负载均衡，返回地址。那么具体使用了哪个负载均衡器，是由.getConfiguration().getLoadBalancer()配置了的！
            InetSocketAddress address = LjwrpcBootstrap.getInstance()
                    .getConfiguration().getLoadBalancer().selectServiceAddress(interfaceRef.getName(), group);// group提供了分组，要在哪个分组里的服务列表里找
            if (log.isDebugEnabled()) {
                log.debug("服务调用方,发现了服务【{}】的可用主机【{}】", interfaceRef.getName(), address);
            }

            // 4.获取当前地址所对应的断路器，如果断路器是打开的，则不发送请求，抛出异常
            Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = LjwrpcBootstrap.getInstance()
                    .getConfiguration().getEveryIpCircuitBreaker();
            CircuitBreaker circuitBreaker = everyIpCircuitBreaker.get(address);
            if (circuitBreaker == null){
                circuitBreaker = new CircuitBreaker(10, 0.5F);
                everyIpCircuitBreaker.put(address, circuitBreaker);
            }

            try {
                // 如果断路器是打开的
                if (ljwrpcRequest.getRequestType() != RequestType.HEART_BEAT.getId() && circuitBreaker.isBreake()) {

                    // 定期打开
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            LjwrpcBootstrap.getInstance()
                                    .getConfiguration().getEveryIpCircuitBreaker()
                                    .get(address).reset();
                        }
                    }, 5000);

                    throw new RuntimeException("当前断路器已经开启，无法发送请求");
                }

                // 5.尝试获取一个可用通道
                Channel channel = getAvailableChannel(address);
                if (log.isDebugEnabled()) {
                    log.debug("获取了和【{}】建立的连接通道，准备发送数据", address);
                }


                /*
                ---------------同步策略（会阻塞）----------------
                 */
//                ChannelFuture channelFuture = channel.writeAndFlush(new Object());
//                // 需要学习ChannelFuture的api:  get阻塞获取结果； getNow获取当前的结果，如果未处理完成，返回null
//                if (channelFuture.isDone()) {
//                    Object object = channelFuture.getNow();
//                } else if (channelFuture.isSuccess()) {
//                    //需要捕获异常，子线程可以捕获异步任务中的异常
//                    Throwable cause = channelFuture.cause();
//                    throw new RuntimeException(cause);
//                }

                /*
                ---------------异步策略（不会阻塞）----------------
                 */

                // 6.写出报文
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                // 将 completableFuture 挂起暴露出去，等待处理结果的返回
                LjwrpcBootstrap.PENDING_REQUEST.put(ljwrpcRequest.getRequestId(), completableFuture);

                // 这里直接writeAndFlush 写出了一个请求，这个请求的实例就会进入pipeline，执行出站的一系列操作
                // 我们可以想象的到，第一个出站的程序一定是将ljwRpcRequest请求对象转化成一个二进制的报文
                channel.writeAndFlush(ljwrpcRequest).addListener((ChannelFutureListener) promise -> {
                    // 当前的promise 将来返回的结果是什么,writeAndFlush的返回结果
                    // 一旦数据被写出去，这个promise就结束了

                    // 只需要处理以下异常就可以了
                    if (!promise.isSuccess()) {
                        completableFuture.completeExceptionally(promise.cause());
                    }
                });

                // 7.清理ThreadLocal
                LjwrpcBootstrap.REQUEST_THREAD_LOCAL.remove();

                // 如果没有地方处理这个completableFuture，这里会阻塞，等待complete方法的执行
                // q：我们需要在哪里调用complete方法得到结果？ 很明显，pipeline中最终的 handler 的处理结果

                // 8.获取响应的结果
                Object result = completableFuture.get(10, TimeUnit.SECONDS);

                // 记录每个发出去的请求，后面判断断路器的开或关用
                circuitBreaker.recordRequest();

                return result;
            } catch (Exception e) {
                // 次数减一，并且等待固定时间，固定时间有一定的问题，容易重试风暴
                tryTimes--;
                // 记录错误的次数
                circuitBreaker.recordErrorRequest();
                try {
                    Thread.sleep(intervalTime);
                } catch (InterruptedException ex) {
                    log.error("在进行重试时发生异常", ex);
                }
                if (tryTimes < 0) {
                    log.error("对方法【{}】进行远程调用时，重试{}次，依然不可调用", method.getName(), tryTimes, e);
                    break;
                }
                log.error("在进行第{}次重试时发生异常", 3 - tryTimes, e);
            }
        }
        throw new RuntimeException("执行远程方法" + method.getName() + "调用失败。");
    }

    /**
     * 根据地址获取一个可用的通道
     *
     * @param address
     * @return
     */
    private Channel getAvailableChannel(InetSocketAddress address) {

        // 1.尝试从缓存中获取
        Channel channel = LjwrpcBootstrap.CHANNEL_CACHE.get(address);

        // 2.拿不到就去建立连接
        if (channel == null) {
            // await方法会阻塞，会等待连接成功再返回，netty还提供了异步处理的逻辑
            // sync和await都是阻塞当前线程，获取返回值（连接的过程是异步的，发送数据的过程是异步的）
            // 如果发生了异常，sync会主动在主线程抛出异常，await不会，异常在子线程中处理，需要使用Future中处理
//                    channel = NettyBootstrapInitializer.getBootstrap().connect(address).await().channel();

            // 使用addListener执行的异步操作
            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();
            NettyBootstrapInitializer.getBootstrap().connect(address).addListener(
                    (ChannelFutureListener) promise -> {
                        if (promise.isDone()) {
                            // 异步的
                            if (log.isDebugEnabled()) {
                                log.debug("已经和【{}】成功建立了连接", address);
                            }
                            channelFuture.complete(promise.channel());
                        } else if (!promise.isSuccess()) {
                            channelFuture.completeExceptionally(promise.cause());
                        }
                    }
            );
            // 阻塞获取channel
            try {
                channel = channelFuture.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("获取通道时发生异常", e);
                throw new DiscoveryException(e);
            }

            // 缓存channel
            LjwrpcBootstrap.CHANNEL_CACHE.put(address, channel);
        }

        if (channel == null) {
            log.error("获取或建立与【{}】的通道时发生了异常", address);
            throw new NetworkException("获取通道时发生了异常");
        }

        return channel;
    }
}
