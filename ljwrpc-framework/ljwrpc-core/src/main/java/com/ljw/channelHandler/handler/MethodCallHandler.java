package com.ljw.channelHandler.handler;

import com.ljw.LjwrpcBootstrap;
import com.ljw.ServiceConfig;
import com.ljw.transport.message.LjwrpcRequest;
import com.ljw.transport.message.RequestPayload;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<LjwrpcRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LjwrpcRequest ljwrpcRequest) throws Exception {
        // 1.获取负载内容
        RequestPayload requestPayload = ljwrpcRequest.getRequestPayload();

        // 2.根据负载内容进行方法调用
        Object object = callTargetMethod(requestPayload);

        // 3.封装响应

        // 4.写出响应
        channelHandlerContext.channel().writeAndFlush(object);
    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parameterValue = requestPayload.getParameterValue();

        // 寻找到匹配的暴露出去的具体的实现
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
