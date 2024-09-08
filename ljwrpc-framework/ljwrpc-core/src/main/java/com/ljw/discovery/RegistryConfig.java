package com.ljw.discovery;

import com.ljw.Constant;
import com.ljw.discovery.impl.NacosRegistry;
import com.ljw.discovery.impl.ZookeeperRegistry;
import com.ljw.exceptions.DiscoveryException;

/**
 * 用来获取注册中心及其类型的，定义：zookeeper://127.0.0.1:2181
 *
 * @author 刘家雯
 * @version 1.0
 */
public class RegistryConfig {

    // 定义连接的url zookeeper://127.0.0.1:2181
    private final String connectString;

    public RegistryConfig(String connectString) {
        this.connectString = connectString;
    }

    /**
     * 可以使用简单工厂来完成
     * @return 返回具体的注册中心实例
     */
    public Registry getRegistry() {
        // 1.获取注册中心的类型
        String registryType = getRegistryType(connectString, true).toLowerCase().trim();
        if (registryType.equals("zookeeper")) {
            String host = getRegistryType(connectString, false);
            return new ZookeeperRegistry(host, Constant.TIME_OUT);
        } else if (registryType.equals("nacos")) {
            String host = getRegistryType(connectString, false);
            return new NacosRegistry(host, Constant.TIME_OUT);
        }
        throw new DiscoveryException("未发现合适的注册中心。");
    }

    private String getRegistryType(String connectString, boolean ifType) {
        String[] typeAndHost = connectString.split("://");
        if (typeAndHost.length != 2){
            throw new RuntimeException("给定的注册中心连接url不合法");
        }
        if (ifType) {
            return typeAndHost[0];
        } else {
            return typeAndHost[1];
        }
    }
}
