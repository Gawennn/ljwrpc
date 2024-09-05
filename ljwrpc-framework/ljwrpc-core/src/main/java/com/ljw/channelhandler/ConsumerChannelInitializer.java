package com.ljw.channelhandler;

import com.ljw.channelhandler.handler.LjwrpcRequestEncoder;
import com.ljw.channelhandler.handler.LjwrpcResponseDecoder;
import com.ljw.channelhandler.handler.MySimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {

        socketChannel.pipeline()
                // netty自带的日志处理器
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                // 消息编码器
                .addLast(new LjwrpcRequestEncoder())
                // 入站的解码器
                .addLast(new LjwrpcResponseDecoder())
                // 处理结果
                .addLast(new MySimpleChannelInboundHandler());
    }
}
