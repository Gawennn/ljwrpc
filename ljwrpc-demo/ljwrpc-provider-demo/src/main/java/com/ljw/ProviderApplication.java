package com.ljw;

import com.ljw.discovery.RegistryConfig;
import com.ljw.impl.HelloLjwrpcImpl;

/**
 * 服务提供方，需要注册服务，启动服务
 * @author 刘家雯
 * @version 1.0
 */
public class ProviderApplication {

    public static void main(String[] args) {

        // 1.封装需要发布的服务
        ServiceConfig<HelloLjwrpc> service = new ServiceConfig<>();
        service.setInterface(HelloLjwrpc.class);
        service.setRef(new HelloLjwrpcImpl());

        // 2.通过启动引导程序，启动服务提供方
        // （1）配置 -- 应用的名称 -- 注册中心 -- 序列化协议 -- 压缩方式
        // （2）发布服务
        LjwrpcBootstrap.getInstance()
                .application("first-ljwrpc-provider")
                // 配置注册中心
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("jdk")
                // 发布服务
                //.publish(service)
                // 扫包批量发布
                .scan("com.ljw.impl")
                // 启动服务
                .start();
    }
}
