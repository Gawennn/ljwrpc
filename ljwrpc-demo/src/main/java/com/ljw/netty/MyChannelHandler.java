package com.ljw.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class MyChannelHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        System.out.println("服务端已经收到了消息: -->" + byteBuf.toString(StandardCharsets.UTF_8));

        //可以通过ctx获取channel
        ctx.channel().writeAndFlush(Unpooled.copiedBuffer("hello client!".getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 出现异常时执行的动作（打印并关闭通道）
        cause.printStackTrace();
        ctx.close();
    }
}
