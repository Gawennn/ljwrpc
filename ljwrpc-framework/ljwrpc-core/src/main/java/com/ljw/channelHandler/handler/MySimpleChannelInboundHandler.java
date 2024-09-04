package com.ljw.channelHandler.handler;

import com.ljw.LjwrpcBootstrap;
import com.ljw.transport.message.LjwrpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<LjwrpcResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LjwrpcResponse ljwrpcResponse) throws Exception {
        // 服务提供方，给予的结果
        Object returnValue = ljwrpcResponse.getBody();
        // 从全局的挂起的请求中寻找与之匹配的待处理的completableFuture
        CompletableFuture<Object> completableFuture = LjwrpcBootstrap.PENDING_REQUEST.get(1L);
        completableFuture.complete(returnValue);

        if (log.isDebugEnabled()) {
            log.debug("已寻找到编号为【{}】的completableFuture，处理响应结果。", ljwrpcResponse.getRequestId());
        }
    }
}
