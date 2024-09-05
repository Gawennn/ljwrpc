package com.ljw.channelhandler.handler;

import com.ljw.LjwrpcBootstrap;
import com.ljw.transport.LjwrpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<LjwrpcResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LjwrpcResponse ljwrpcResponse) {

        // 服务提供方，给予的结果
        Object returnValue = ljwrpcResponse.getBody();

        // TODO 需要针对响应码code做处理
        returnValue = returnValue == null ? new Object() : returnValue;

        // 从全局的挂起的请求中寻找与之匹配的待处理的completableFuture
        CompletableFuture<Object> completableFuture = LjwrpcBootstrap.PENDING_REQUEST.get(ljwrpcResponse.getRequestId());
        completableFuture.complete(returnValue);

        if (log.isDebugEnabled()) {
            log.debug("已寻找到编号为【{}】的completableFuture，处理响应结果。", ljwrpcResponse.getRequestId());
        }
    }
}
