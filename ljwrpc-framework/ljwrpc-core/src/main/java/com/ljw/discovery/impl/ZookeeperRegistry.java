package com.ljw.discovery.impl;

import com.ljw.Constant;
import com.ljw.LjwrpcBootstrap;
import com.ljw.ServiceConfig;
import com.ljw.discovery.AbstractRegistry;
import com.ljw.exceptions.DiscoveryException;
import com.ljw.utils.NetUtils;
import com.ljw.utils.zookeeper.ZookeeperNode;
import com.ljw.utils.zookeeper.ZookeeperUtils;
import com.ljw.watch.UpAndDownWatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 配置一个Zookeeper注册中心，使用了ZookeeperUtils工具类
 *
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

        // 建立服务名称的节点
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterface().getName();
        // 这个节点是一个持久节点
        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        // 建立分组节点
        parentNode = parentNode + "/" + service.getGroup();
        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        // 创建本机的临时节点, ip:port ,
        // 服务提供方的端口一般自己设定，我们还需要一个获取ip的方法
        // ip我们通常是需要一个局域网ip，不是127.0.0.1,也不是ipv6
        String node = parentNode + "/" + NetUtils.getIp() + ":" + LjwrpcBootstrap.getInstance().getConfiguration().getPort();
        if(!ZookeeperUtils.exists(zooKeeper,node,null)){
            ZookeeperNode zookeeperNode = new ZookeeperNode(node,null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }

        if (log.isDebugEnabled()){
            log.debug("服务【{}】，已经被注册", service.getInterface().getName());
        }
    }

    /**
     * 注册中心的核心目的是什么？拉取合适的服务列表
     * @param servicename 服务的名称
     * @return 服务列表
     */
    @Override
    public List<InetSocketAddress> lookup(String servicename, String group) {

        // 1.找到服务对应的节点
        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + servicename + "/" + group;

        // 2.从zk中获取他的子节点
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, new UpAndDownWatcher());
        // 获取了所有的可用的服务列表
        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipString -> { //stream流把集合元素看成一种流，借助stream中的api可对其进行各种操作。map转换映射
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.valueOf(ipAndPort[1]);
            return new InetSocketAddress(ip, port);
        }).toList();

        if (inetSocketAddresses.size() == 0){
            throw new DiscoveryException("未发现任何可用的服务主机");
        }

        return inetSocketAddresses;
    }
}
