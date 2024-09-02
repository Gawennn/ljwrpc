package com.ljw;

import lombok.extern.slf4j.Slf4j;

import java.lang.module.ResolvedModule;
import java.util.List;
import java.util.logging.Handler;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class LjwrpcBootstrap {

    // LjwrpcBootstrap是一个单例，我们希望每个应用程序只有一个实
    private static LjwrpcBootstrap ljwrpcBootstrap = new LjwrpcBootstrap();

    public LjwrpcBootstrap() {
        // 构造启动引导程序时，需要做一些什么初始化的事
    }

    public static LjwrpcBootstrap getInstance() {
        return ljwrpcBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     * @param appName 应用的名字
     * @return this 当前实例
     */
    public LjwrpcBootstrap application(String appName) {
        return this;
    }

    /**
     * 用来配置一个注册中心
     * @param registryConfig 注册中心
     * @return this 当前实例
     */
    public LjwrpcBootstrap registry(RegistryConfig registryConfig) {
        return this;
    }

    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig 协议的封装
     * @return this 当前实例
     */
    public LjwrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        if (log.isDebugEnabled()){
            log.debug("当前工程使用了：{}协议进行了序列化" + protocolConfig.toString());
        }
        return this;
    }

    /**
     * --------------------------------------服务提供方的相关api------------------------------------------
     */

    /**
     * 服务发布，将接口=》实现 注册到服务中心
     * @param service 封装的需要发布的服务
     * @return this 当前实例
     */
    public LjwrpcBootstrap publish(ServiceConfig<?> service) {
        if (log.isDebugEnabled()){
            log.debug("服务{}已经被注册" + service.getInterface().getName());
        }
        return this;
    }

    /**
     * 批量发布服务
     * @param services 封装的需要发布的服务的集合
     * @return this 当前实例
     */
    public LjwrpcBootstrap publish(List<? > services) {
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {

    }

    /**
     * --------------------------------------服务调用方的相关api------------------------------------------
     */

    public LjwrpcBootstrap reference(ReferenceConfig<?> reference) {

        // 在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        return this;
    }
}
