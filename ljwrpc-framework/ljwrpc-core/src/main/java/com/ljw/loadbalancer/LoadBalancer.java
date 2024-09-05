package com.ljw.loadbalancer;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 负载均衡器的接口
 *
 * @author 刘家雯
 * @version 1.0
 */
public interface LoadBalancer {

    // 他应该具备的能力，根据服务列表找到一个可用的服务

    /**
     * 根据服务名获取一个可用的服务
     * @param serviceName 服务名称
     * @return 服务地址
     */
    InetSocketAddress selectServiceAddress(String serviceName);

}
