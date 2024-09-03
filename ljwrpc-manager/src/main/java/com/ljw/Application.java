package com.ljw;

import com.ljw.utils.zookeeper.ZookeeperNode;
import com.ljw.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.util.List;

/**
 * 注册中心的管理页面
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class Application {

    public static void main(String[] args) {

        // 帮我们创建基础目录

        // 创建一个zookeeper实例
        ZooKeeper zooKeeper = ZookeeperUtils.createZookeeper();

        // 定义节点和数据
        String basePath = "/ljwrpc-metadata";
        String providersPath = basePath + "/providers";
        String consumersPath = basePath + "/consumers";
        ZookeeperNode baseNode = new ZookeeperNode("/ljwrpc-metadata", null);
        ZookeeperNode providersNode = new ZookeeperNode(providersPath, null);
        ZookeeperNode consumersNode = new ZookeeperNode(consumersPath, null);
        // 创建节点
        List.of(baseNode, providersNode, consumersNode).forEach(node -> {
            ZookeeperUtils.createNode(zooKeeper, node, null, CreateMode.PERSISTENT);
        });

        // 关闭连接
        ZookeeperUtils.close(zooKeeper);
    }
}
