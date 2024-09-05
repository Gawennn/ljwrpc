package com.ljw.core;

import com.ljw.LjwrpcBootstrap;
import com.ljw.NettyBootstrapInitializer;
import com.ljw.compress.CompressorFactory;
import com.ljw.discovery.Registry;
import com.ljw.enumeration.RequestType;
import com.ljw.serialize.SerializerFactory;
import com.ljw.transport.LjwrpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String servicename) {
        // 1、从注册中心拉取服务列表并建立连接
        Registry registry = LjwrpcBootstrap.getInstance().getRegistry();
        List<InetSocketAddress> addresses = registry.lookup(servicename);

        // 2、将连接进行缓存
        for (InetSocketAddress address : addresses) {
            try {
                if (LjwrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    Channel channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    LjwrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 3、任务，定期发送消息
        Thread thread = new Thread(() -> new Timer().scheduleAtFixedRate(new MyTimerTask(),0, 2000)
        , "ljwrpc-HeartDetector-thread");
        thread.setDaemon(true); // 开启一个守护线程去心跳检测。不让主线程阻塞
        thread.start();

    }

    private static class MyTimerTask extends TimerTask {

        @Override
        public void run() {

            // 将响应时长的map清空
            LjwrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.clear();

            // 遍历所有的cahnnel
            Map<InetSocketAddress, Channel> cache = LjwrpcBootstrap.CHANNEL_CACHE;
            for (Map.Entry<InetSocketAddress, Channel> entry : cache.entrySet()) {
                Channel channel = entry.getValue();

                long start = System.currentTimeMillis();
                // 构建一个心跳请求
                LjwrpcRequest ljwrpcRequest = LjwrpcRequest.builder()
                        .requestId(LjwrpcBootstrap.ID_GENERATOR.getId())
                        .compressType(CompressorFactory.getCompressor(LjwrpcBootstrap.COMPRESS_TYPE).getCode())
                        .requestType(RequestType.HEART_BEAT.getId())
                        .serializeType(SerializerFactory.getSerializer(LjwrpcBootstrap.SERIALIZE_TYPE).getCode())
                        .timeStamp(start)
                        .build();

                // 4.写出报文
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                // 将 completableFuture 暴露出去
                LjwrpcBootstrap.PENDING_REQUEST.put(ljwrpcRequest.getRequestId(), completableFuture);

                channel.writeAndFlush(ljwrpcRequest).addListener( (ChannelFutureListener) promise ->{
                    if (!promise.isSuccess()) {
                        completableFuture.completeExceptionally(promise.cause());
                    }
                });

                //
                Long endTime = 0L;
                try {
                    completableFuture.get();
                    endTime = System.currentTimeMillis();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                Long time = endTime - start;

                // 使用treemap进行缓存
                LjwrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(time, channel);
                log.debug("和【{}】服务器的响应时间是【{}】", entry.getKey(), time);
            }

            log.info("--------------------------------------响应时间的treemap-----------------------------------");
            for (Map.Entry<Long, Channel> entry : LjwrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.entrySet()) {
                if (log.isDebugEnabled()) {
                    log.debug("【{}】--->channelId:【{}】", entry.getKey(), entry.getValue().id());
                }
            }
        }
    }
}
