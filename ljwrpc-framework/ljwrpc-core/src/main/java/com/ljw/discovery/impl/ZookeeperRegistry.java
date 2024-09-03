package com.ljw.discovery.impl;

import com.ljw.Constant;
import com.ljw.ServiceConfig;
import com.ljw.discovery.AbstractRegistry;
import com.ljw.exceptions.DiscoveryException;
import com.ljw.exceptions.NetworkException;
import com.ljw.utils.NetUtils;
import com.ljw.utils.zookeeper.ZookeeperNode;
import com.ljw.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {

    //维护一个zk实例
    private ZooKeeper zooKeeper;

    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public ZookeeperRegistry(String connectString, int timeout) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(connectString, timeout);
    }

    @Override
    public void registry(ServiceConfig<?> service) {

        // 服务名称的节点
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterface().getName();
        // 这个节点是一个持久节点
        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        // 创建本机的临时节点, ip:port ,
        // 服务提供方的端口一般自己设定，我们还需要一个获取ip的方法
        // ip我们通常是需要一个局域网ip，不是127.0.0.1，也不是ipv6
        //TODO 后续处理端口问题
        String node = parentNode + "/" + NetUtils.getIp() + ":" + port;
        if (!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }

        if (log.isDebugEnabled()){
            log.debug("服务{}已经被注册" + service.getInterface().getName());
        }
    }

    @Override
    public InetSocketAddress lookup(String servicename) {
        // 1.找到服务对应的节点
        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + servicename;

        // 2.从zk中获取他的子节点
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, null);
        // 获取了所有的可用的服务列表
        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipString -> {
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.valueOf(ipAndPort[1]);
            return new InetSocketAddress(ip, port);
        }).toList();

        if (inetSocketAddresses.size() == 0){
            throw new DiscoveryException("未发现任何可用的服务主机");
        }
        // TODO q:我们每次调用相关方法的时候，都需要去注册中心拉取服务列表吗? 本地缓存 + watcher
        // TODO q:我们如何合理的选择一个可用的服务，而不是只获取第一个? 负载均衡策略

        return inetSocketAddresses.get(0);
    }
}
