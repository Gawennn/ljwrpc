package com.ljw.loadbalancer.impl;

import com.ljw.LjwrpcBootstrap;
import com.ljw.discovery.Registry;
import com.ljw.exceptions.LoadBalancerException;
import com.ljw.loadbalancer.AbstractLoadBalancer;
import com.ljw.loadbalancer.LoadBalancer;
import com.ljw.loadbalancer.Selector;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询的负载均衡策略
 *
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new RoundRobinSelector(serviceList);
    }

    private static class RoundRobinSelector implements Selector{
        private List<InetSocketAddress> serviceList;
        private AtomicInteger index;


        public RoundRobinSelector(List<InetSocketAddress> serviceList) {
            this.serviceList = serviceList;
            this.index = new AtomicInteger(0);
        }

        @Override
        public InetSocketAddress getNext() {
            if (serviceList == null || serviceList.size() == 0) {
                log.error("进行负载均衡选取节点时发现服务列表为空。");
                throw new LoadBalancerException();
            }

            InetSocketAddress address = serviceList.get(index.get());

            // 如果他到了最后一个位置，重制
            if (index.get() == serviceList.size() - 1) {
                index.set(0);
            } else {
                // 游标后移一位
                index.incrementAndGet();
            }

            return address;
        }

    }
}
