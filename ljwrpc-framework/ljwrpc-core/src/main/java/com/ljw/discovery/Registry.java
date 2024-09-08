package com.ljw.discovery;

import com.ljw.ServiceConfig;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 注册中心应该具有什么样的能力
 *
 * @author 刘家雯
 * @version 1.0
 */
public interface Registry {

    /**
     * 注册服务
     * @param serviceConfig 服务的配置内容
     */
    void registry(ServiceConfig<?> serviceConfig);

    /**
     * 从注册中心拉取服务列表
     * @param servicename 服务的名称
     * @return 服务的地址
     */
    List<InetSocketAddress> lookup(String servicename, String group);
}
