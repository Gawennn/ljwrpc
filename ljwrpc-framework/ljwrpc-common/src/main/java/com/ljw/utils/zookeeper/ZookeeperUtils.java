package com.ljw.utils.zookeeper;

import com.ljw.Constant;
import com.ljw.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class ZookeeperUtils {

    /**
     * 使用默认配置创建zookeeper实例
     * @return
     */
    public static ZooKeeper createZookeeper(){
        // 定义连接参数
        String connectString = Constant.DEFAULT_ZK_CONNECT;
        // 定义超时时间
        int timeout = Constant.TIME_OUT;
        return createZookeeper(connectString, timeout);
    }

    public static ZooKeeper createZookeeper(String connectString, int timeout){
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            // 创建zk实例，建立连接
            final ZooKeeper zooKeeper = new ZooKeeper(connectString, timeout, event -> {
                // 只有连接成功才放行
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("客户端已经连接成功");
                    countDownLatch.countDown();
                }
            }); // 构建zookeeper

            countDownLatch.await();

            return zooKeeper;
        } catch (IOException | InterruptedException e) {
            log.error("创建zookeeper实例时，产生异常，如下: ", e);
            throw new ZookeeperException();
        }
    }

    /**
     * 创建一个节点的工具方法
     * @param zooKeeper zookeeper实例
     * @param node 节点
     * @param watcher watcher实例
     * @param createMode 节点的类型
     * @return true：成功创建；false：已经存在；异常：抛出
     */
    public static Boolean createNode(ZooKeeper zooKeeper, ZookeeperNode node, Watcher watcher, CreateMode createMode){
        try {
            if (zooKeeper.exists(node.getNodePath(), watcher) == null) {
                String result = zooKeeper.create(node.getNodePath(), node.getData(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                log.info("根节点【{}】,成功创建", result);
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.info("节点【{}】已经存在，无需创建。", node.getNodePath());
                }
                return false;
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("创建基础目录时，发生异常：", e);
            throw new ZookeeperException();
        }
    }

    /**
     * 关闭zookeeper的方法
     * @param zooKeeper zookeeper实例
     */
    public static void close(ZooKeeper zooKeeper) {
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            log.error("关闭zookeeper时，发生异常：", e);
            throw new ZookeeperException();
        }
    }

    /**
     * 判断节点是否存在
     * @param zk zk实例
     * @param node 节点路径
     * @param watcher watcher
     * @return true：存在      false：不存在
     */
    public static boolean exists(ZooKeeper zk, String node, Watcher watcher) {
        try {
            return zk.exists(node, watcher) != null;
        } catch (KeeperException | InterruptedException e) {
            log.error("判断节点【{}】是否存在是发生异常", node, e);
            throw new ZookeeperException(e);
        }
    }

    /**
     * 查询一个节点的子元素
     * @param zooKeeper zk实例
     * @param serviceNode 服务节点
     * @return 子元素列表
     */
    public static List<String> getChildren(ZooKeeper zooKeeper, String serviceNode, Watcher watcher) {
        try {
            return zooKeeper.getChildren(serviceNode, watcher);
        } catch (KeeperException |InterruptedException e) {
            log.error("获取节点【{}】的子元素时发生异常。", serviceNode, e);
            throw new ZookeeperException(e);
        }
    }
}
