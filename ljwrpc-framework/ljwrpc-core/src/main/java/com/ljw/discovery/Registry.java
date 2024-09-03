package com.ljw.discovery;

import com.ljw.ServiceConfig;

import java.net.InetSocketAddress;

/**
 * 思考：注册中心应该具有什么样的能力
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
     * 从注册中心拉取一个可用的服务
     * @param servicename 服务的名称
     * @return 服务的地址
     */
    InetSocketAddress lookup(String servicename);
}
