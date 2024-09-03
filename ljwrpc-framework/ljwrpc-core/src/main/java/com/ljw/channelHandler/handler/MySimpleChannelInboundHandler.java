package com.ljw.channelHandler.handler;

import com.ljw.LjwrpcBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
        // 服务提供方，给予的结果
        String result = msg.toString(Charset.defaultCharset());
        // 从全局的挂起的请求中寻找与之匹配的待处理的cf
        CompletableFuture<Object> completableFuture = LjwrpcBootstrap.PENDING_REQUEST.get(1L);
        completableFuture.complete(result);
    }
}
