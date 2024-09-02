package com.ljw;

import com.ljw.netty.MyWatcher;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class ZookeeperTest {

    ZooKeeper zooKeeper;
    CountDownLatch countDownLatch = new CountDownLatch(1);

    @Before
    public void createZk() {

        // 定义连接参数
        String connectString = "172.16.198.130:2181,172.16.198.132:2181,172.16.198.133:2181";
        // 定义超时时间
        int timeout = 100000;

        try {
            // new MyWatcher()是一个默认的watcher
//            zooKeeper = new ZooKeeper(connectString, timeout, new MyWatcher()); // 构建zookeeper
            // 构建zk是否需要等待时间
            zooKeeper = new ZooKeeper(connectString, timeout, event -> {
                // 只有连接成功才放行
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("客户端已经连接成功");
                    countDownLatch.countDown();
                }
            }); // 构建zookeeper
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCreatePNode() {
        try {
            // 会等待连接成功
            countDownLatch.await();
            String result = zooKeeper.create("/ljw", "hello".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("result = " + result);;
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zooKeeper != null) {
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testDeletePNode() {
        try {
            Stat stat = zooKeeper.exists("/ljw", null);
            // 获取版本号（当前节点的数据版本）
            int version = stat.getVersion();

            // 当前节点的acl数据版本
            int aversion = stat.getAversion();

            // 当前子节点数据的版本
            int cversion = stat.getCversion();

            // version -1表示无视版本号
            zooKeeper.delete("/ljw", -1);

        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zooKeeper != null) {
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testWatcher(){
        try {
            // 以下三个方法可以注册watcher，可以直接new一个新的watcher
            // 也可以直接使用true选定一个默认的watcher
            zooKeeper.exists("/ljw", true);
//            zooKeeper.getChildren();
//            zooKeeper.getData();

            while (true){
                Thread.sleep(10000);
            }

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zooKeeper != null) {
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
