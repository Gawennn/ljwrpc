package com.ljw.channelHandler.handler;

import com.ljw.LjwrpcBootstrap;
import com.ljw.ServiceConfig;
import com.ljw.enumeration.ResponseCode;
import com.ljw.transport.message.LjwrpcRequest;
import com.ljw.transport.message.LjwrpcResponse;
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
        Object result = callTargetMethod(requestPayload);

        if (log.isDebugEnabled()) {
            log.debug("请求【{}】已经在服务端完成方法调用。", ljwrpcRequest.getRequestId());
        }

        // 3.封装响应
        LjwrpcResponse ljwrpcResponse = new LjwrpcResponse();
        ljwrpcResponse.setCode(ResponseCode.SUCCESS.getCode());
        ljwrpcResponse.setRequestId(ljwrpcRequest.getRequestId());
        ljwrpcResponse.setCompressType(ljwrpcRequest.getCompressType());
        ljwrpcResponse.setSerializeType(ljwrpcRequest.getSerializeType());
        ljwrpcResponse.setBody(result);

        // 4.写出响应
        channelHandlerContext.channel().writeAndFlush(ljwrpcResponse);
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
