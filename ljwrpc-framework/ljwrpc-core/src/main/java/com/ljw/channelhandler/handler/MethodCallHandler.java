package com.ljw.channelhandler.handler;

import com.ljw.LjwrpcBootstrap;
import com.ljw.ServiceConfig;
import com.ljw.core.ShutDownHolder;
import com.ljw.enumeration.RequestType;
import com.ljw.enumeration.ResponseCode;
import com.ljw.protection.RateLimiter;
import com.ljw.protection.TokenBuketRateLimiter;
import com.ljw.transport.LjwrpcRequest;
import com.ljw.transport.LjwrpcResponse;
import com.ljw.transport.RequestPayload;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Map;

/**
 * 服务端进行方法调用的handler
 *
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<LjwrpcRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LjwrpcRequest ljwrpcRequest) {

        // 1.先封装部分响应
        LjwrpcResponse ljwrpcResponse = new LjwrpcResponse();
        ljwrpcResponse.setRequestId(ljwrpcRequest.getRequestId());
        ljwrpcResponse.setCompressType(ljwrpcRequest.getCompressType());
        ljwrpcResponse.setSerializeType(ljwrpcRequest.getSerializeType());

        // 2.获得通道
        Channel channel = channelHandlerContext.channel();

        // 3.查看关闭的挡板是否打开，如果挡板已经打开，返回一个错误的响应
        if (ShutDownHolder.BAFFLE.get()) {
            ljwrpcResponse.setCode(ResponseCode.CLOSING.getCode());
            channel.writeAndFlush(ljwrpcResponse);
            return;
        }

        // 4.计数器加一
        ShutDownHolder.REQUEST_COUNTER.increment();


        // 5.完成限流相关的操作
        SocketAddress socketAddress = channel.remoteAddress();
        Map<SocketAddress, RateLimiter> everyIpRateLimiter =
                LjwrpcBootstrap.getInstance().getConfiguration().getEveryIpRateLimiter();

        RateLimiter rateLimiter = everyIpRateLimiter.get(socketAddress);
        if (rateLimiter == null) {
            rateLimiter = new TokenBuketRateLimiter(5, 5);
            everyIpRateLimiter.put(socketAddress, rateLimiter);
        }
        boolean allowRequest = rateLimiter.allowRequest();

        // 6.处理请求的逻辑
        // 限流
        if (!allowRequest) {
            // 需要封装响应 并且返回了
            ljwrpcResponse.setCode(ResponseCode.RATE_LIMIT.getCode());
            // 处理心跳
        } else if (ljwrpcRequest.getRequestType() == RequestType.HEART_BEAT.getId()) {
            // 需要封装响应并且返回
            ljwrpcResponse.setCode(ResponseCode.SUCCESS_HEART_BEAT.getCode());
        } else { // 正常调用
            /** ---------------------------------具体的调用过程---------------------------------**/
            // （1）获取负载内容
            RequestPayload requestPayload = ljwrpcRequest.getRequestPayload();

            // （2）根据负载内容进行方法调用
            try {
                Object result = callTargetMethod(requestPayload);
                if (log.isDebugEnabled()) {
                    log.debug("编号为【{}】的请求已经在服务端完成方法调用。", ljwrpcRequest.getRequestId());
                }
                // （3）封装响应   我们是否需要考虑另外一个问题，响应码，响应类型
                ljwrpcResponse.setCode(ResponseCode.SUCCESS.getCode());
                ljwrpcResponse.setBody(result);
            } catch (Exception e){
                log.error("编号为【{}】的请求在调用过程中发生异常。", ljwrpcRequest.getRequestId(), e);
                ljwrpcResponse.setCode(ResponseCode.FAIL.getCode());
            }

            // 6.写出响应
            channel.writeAndFlush(ljwrpcResponse);

            // 7.计数器减一
            ShutDownHolder.REQUEST_COUNTER.decrement();
        }
    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parameterValue = requestPayload.getParameterValue();

        // 寻找到匹配的暴露出去的服务的具体实现
        ServiceConfig<?> serviceConfig = LjwrpcBootstrap.SERVERS_LIST.get(interfaceName);
        Object refImpl = serviceConfig.getRef();

        // 通过反射调用  1.获取方法对象  2.执行invoke方法
        Object returnValue;
        try {
            Class<?> aClass = refImpl.getClass();
            Method method = aClass.getMethod(methodName, parametersType);
            returnValue = method.invoke(refImpl, parameterValue);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.error("调用服务【{}】的方法【{}】时发生了异常。", interfaceName, methodName, e);
            throw new RuntimeException(e);
        }
        return null;
    }
}
