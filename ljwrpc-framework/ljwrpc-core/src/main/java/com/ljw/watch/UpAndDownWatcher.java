package com.ljw.watch;

import com.ljw.LjwrpcBootstrap;
import com.ljw.NettyBootstrapInitializer;
import com.ljw.discovery.Registry;
import com.ljw.loadbalancer.LoadBalancer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class UpAndDownWatcher implements Watcher {

    @Override
    public void process(WatchedEvent event) {

        // 当前的阶段是否发生了变化
        if (event.getType() == Event.EventType.NodeDataChanged) {
            if (log.isDebugEnabled()) {
                log.debug("检测到服务【{}】有节点上/下线，将重新拉取服务列表。。。", event.getPath());
            }
            String serviceName = getServiceName(event.getPath());
            Registry registry = LjwrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
            List<InetSocketAddress> addresses = registry.lookup(serviceName,
                    LjwrpcBootstrap.getInstance().getConfiguration().getGroup());
            // 处理新增的节点
            for (InetSocketAddress address : addresses) {
                // 新增的节点 会在address 不在CHANNEL_CACHE
                // 下线的节点 可能会在CHANNEL_CACHE 不在address
                if (!LjwrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    // 根据地址建立连接，并且缓存
                    Channel channel = null;
                    try {
                        channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    LjwrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            }

            // 处理下线的节点 可能会在CHANNEL_CACHE  不在address
            for (Map.Entry<InetSocketAddress, Channel> entry : LjwrpcBootstrap.CHANNEL_CACHE.entrySet()) {
                if (!addresses.contains(entry.getKey())) {
                    LjwrpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                }
            }

            // 获得负载均衡器，进行重新的laodbalance
            LoadBalancer loadBalancer = LjwrpcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            loadBalancer.reLoadBalance(serviceName, addresses);

        }
    }

    private String getServiceName(String path) {
        String[] split = path.split("/");
        return split[split.length - 1];
    }
}
