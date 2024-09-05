package com.ljw.loadbalancer.impl;

import com.ljw.LjwrpcBootstrap;
import com.ljw.exceptions.LoadBalancerException;
import com.ljw.loadbalancer.AbstractLoadBalancer;
import com.ljw.loadbalancer.Selector;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class MinimumResponseTimeLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new MinimumResponseTimeSelector(serviceList);
    }

    private static class MinimumResponseTimeSelector implements Selector {

        public MinimumResponseTimeSelector(List<InetSocketAddress> serviceList) {

        }

        @Override
        public InetSocketAddress getNext() {

            Map.Entry<Long, Channel> entry = LjwrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
            if (entry != null) {
                return (InetSocketAddress) entry.getValue().remoteAddress();
            }

            // 直接从缓存中获取一个可用的就行了
            Channel channel = (Channel)LjwrpcBootstrap.CHANNEL_CACHE.values().toArray()[0];
            return (InetSocketAddress) channel.remoteAddress();
        }

        @Override
        public void reBalance() {

        }
    }
}
