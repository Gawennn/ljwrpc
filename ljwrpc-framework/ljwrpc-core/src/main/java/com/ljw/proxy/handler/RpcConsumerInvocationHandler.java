package com.ljw.proxy.handler;

import com.ljw.LjwrpcBootstrap;
import com.ljw.NettyBootstrapInitializer;
import com.ljw.discovery.Registry;
import com.ljw.enumeration.RequestType;
import com.ljw.exceptions.DiscoveryException;
import com.ljw.exceptions.NetworkException;
import com.ljw.proxy.serialize.Serializer;
import com.ljw.proxy.serialize.SerializerFactory;
import com.ljw.transport.message.LjwrpcRequest;
import com.ljw.transport.message.RequestPayload;
import com.ljw.utils.IdGenerator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
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

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 我们调用sayHi方法，事实上会走进这个代码段中
        // 我们已经知道 method args
//        log.info("method-->{}", method.getName());
//        log.info("args-->{}", args);

        // 1. 发现服务，从注册中心寻找一个可用的服务
        // 传入服务的名字，返回ip+端口
        InetSocketAddress address = registry.lookup(interfaceRef.getName());
        if (log.isDebugEnabled()){
            log.debug("服务调用方,发现了服务【{}】的可用主机【{}】", interfaceRef.getName(), address);
        }

        // 2.使用netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表 得到结果
        // 定义线程池，EventLoopGroup
        // 整个连接过程放在这里行不行，也就意味着每个调用都要产生一个新的netty连接。如何缓存我们的连接，
        // 也就意味着每次在此处建立一个新的连接是不合适的

        // 解决方案？缓存channel。尝试从缓存中获取channel。如果未获取，创建新的连接并获取
        // 2.尝试获取一个可用通道
        Channel channel = getAvailableChannel(address);
        if (log.isDebugEnabled()){
            log.debug("获取了和【{}】建立的连接通道，准备发送数据", address);
        }


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

        LjwrpcRequest ljwrpcRequest = LjwrpcRequest.builder()
                .requestId(LjwrpcBootstrap.ID_GENERATOR.getId())
                .compressType(SerializerFactory.getSerializer(LjwrpcBootstrap.SERIALIZE_TYPE).getCode())
                .requestType(RequestType.REQUEST.getId())
                .serializeType(SerializerFactory.getSerializer(LjwrpcBootstrap.SERIALIZE_TYPE).getCode())
                .requestPayload(requestPayload)
                .build();

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

        // 4.写出报文
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        // 将 completableFuture 暴露出去
        LjwrpcBootstrap.PENDING_REQUEST.put(1L, completableFuture);

        // 这里直接writeAndFlush 写出了一个请求，这个请求的实例就会进入pipeline，执行出站的一系列操作
        // 我们可以想象的到，第一个出站的程序一定是将ljwRpcRequest请求对象转化成一个二进制的报文
        channel.writeAndFlush(ljwrpcRequest).addListener( (ChannelFutureListener) promise ->{
            // 当前的promise 将来返回的结果是什么,writeAndFlush的返回结果
            // 一旦数据被写出去，这个promise就结束了
//                    if (promise.isDone()) {
//                        completableFuture.complete(promise.getNow());
//                    }

            // 只需要处理以下异常就可以了
            if (!promise.isSuccess()) {
                completableFuture.completeExceptionally(promise.cause());
            }
        });

        // 如果没有地方处理这个completableFuture，这里会阻塞，等待complete方法的执行
        // q：我们需要在哪里调用complete方法得到结果？ 很明显，pipeline中最终的 handler 的处理结果
        // 5.获取响应的结果
        return completableFuture.get(3, TimeUnit.SECONDS);
    }

    /**
     * 根据地址获取一个可用的通道
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
                        if (promise.isDone()){
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
