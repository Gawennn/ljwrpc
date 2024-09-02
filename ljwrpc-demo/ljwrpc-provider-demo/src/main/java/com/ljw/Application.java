package com.ljw;

import com.ljw.impl.HelloLjwrpcImpl;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class Application {

    public static void main(String[] args) {

        //服务提供方，需要注册服务，启动服务

        // 1.封装需要发布的服务
        ServiceConfig<HelloLjwrpc> service = new ServiceConfig<>();
        service.setInterface(HelloLjwrpc.class);
        service.setRef(new HelloLjwrpcImpl());
        // 2.定义注册中心

        // 2.通过启动引导程序，启动服务提供方
        // （1）配置 -- 应用的名称 -- 注册中心 -- 序列化协议 -- 压缩方式
        // （2）发布服务
        LjwrpcBootstrap.getInstance()
                .application("first-ljwrpc-provider")
                // 配置注册中心
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                // 配置协议
                .protocol(new ProtocolConfig("jdk"))
                // 发布服务
                .publish(service)
                .start();
    }
}
