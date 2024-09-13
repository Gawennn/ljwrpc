package com.ljw.channelhandler.handler;

import com.ljw.LjwrpcBootstrap;
import com.ljw.enumeration.ResponseCode;
import com.ljw.exceptions.ResponseException;
import com.ljw.loadbalancer.LoadBalancer;
import com.ljw.protection.CircuitBreaker;
import com.ljw.transport.LjwrpcRequest;
import com.ljw.transport.LjwrpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 此类为客户端方简单的入站消息处理器
 * 用于处理服务器端返回的 LjwrpcResponse，并根据返回的响应码和状态执行对应的逻辑操作
 *
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<LjwrpcResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LjwrpcResponse ljwrpcResponse) {

        // 从全局的挂起的请求中寻找与之匹配的待处理的completableFuture
        // 获取当前响应对应的请求ID，通过请求ID可以找到发出请求时生成的 CompletableFuture 对象
        CompletableFuture<Object> completableFuture = LjwrpcBootstrap.PENDING_REQUEST.get(ljwrpcResponse.getRequestId());

        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = LjwrpcBootstrap.getInstance()
                .getConfiguration().getEveryIpCircuitBreaker();
        CircuitBreaker circuitBreaker = everyIpCircuitBreaker.get(socketAddress);

        byte code = ljwrpcResponse.getCode();
        if (code == ResponseCode.FAIL.getCode()){
            circuitBreaker.recordErrorRequest();
            // complete显示地标记这个completableFuture任务已经完成
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求，返回错误的结果，响应码【{}】",
                    ljwrpcResponse.getRequestId(), ljwrpcResponse.getCode());
            throw new ResponseException(code, ResponseCode.FAIL.getDesc());

        } else if (code == ResponseCode.RATE_LIMIT.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求，被限流，响应码【{}】",
                    ljwrpcResponse.getRequestId(), ljwrpcResponse.getCode());
            throw new ResponseException(code, ResponseCode.RATE_LIMIT.getDesc());

        } else if (code == ResponseCode.RESOURCE_NOT_FOUND.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求，未找到目标资源，响应码【{}】",
                    ljwrpcResponse.getRequestId(), ljwrpcResponse.getCode());
            throw new ResponseException(code, ResponseCode.RESOURCE_NOT_FOUND.getDesc());

        } else if (code == ResponseCode.SUCCESS.getCode()) {
            // 服务提供方，给予的结果
            Object returnValue = ljwrpcResponse.getBody();
            completableFuture.complete(returnValue);
            if (log.isDebugEnabled()) {
                log.debug("已寻找到编号为【{}】的completableFuture，处理响应结果。", ljwrpcResponse.getRequestId());
            }
        } else if (code == ResponseCode.SUCCESS_HEART_BEAT.getCode()) {
            // 服务提供方，给予的结果
            completableFuture.complete(null);
            if (log.isDebugEnabled()) {
                log.debug("已寻找到编号为【{}】的completableFuture，处理心跳检测，处理响应结果。", ljwrpcResponse.getRequestId());
            }
        } else if (code == ResponseCode.CLOSING.getCode()) {
            completableFuture.complete(null);
            if (log.isDebugEnabled()) {
                log.debug("当前id为【{}】的请求，访问被拒绝，目标服务器正处于关闭中，响应码【{}】",
                        ljwrpcResponse.getRequestId(), ljwrpcResponse.getCode());
            }

            // 修正负载均衡器
            // 从健康列表中移除
            LjwrpcBootstrap.CHANNEL_CACHE.remove(socketAddress);
            // 找到负载均衡器进行reLoadBalance
            LoadBalancer loadBalancer = LjwrpcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            // 重新进行负载均衡
            LjwrpcRequest ljwrpcRequest = LjwrpcBootstrap.REQUEST_THREAD_LOCAL.get();
            loadBalancer.reLoadBalance(ljwrpcRequest.getRequestPayload().getInterfaceName(),
                    LjwrpcBootstrap.CHANNEL_CACHE.keySet().stream().toList());

            throw new ResponseException(code, ResponseCode.CLOSING.getDesc());
        }
    }
}
