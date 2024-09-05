package com.ljw.loadbalancer;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author 刘家雯
 * @version 1.0
 */
public interface Selector {

    /**
     * 根据服务列表执行一种算法获取一个服务节点
     * @return 具体的服务节点
     */
    InetSocketAddress getNext();

    // TODO 服务动态上下线需要进行reBalance
    void reBalance();

}
