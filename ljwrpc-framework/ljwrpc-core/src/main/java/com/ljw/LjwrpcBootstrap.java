package com.ljw;

import com.ljw.discovery.Registry;
import com.ljw.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class LjwrpcBootstrap {

    // LjwrpcBootstrap是一个单例，我们希望每个应用程序只有一个实
    private static LjwrpcBootstrap ljwrpcBootstrap = new LjwrpcBootstrap();

    // 定义相关的一些基础配置
    private String appName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private int port = 8088;

    // 注册中心
    private Registry registry;

    // 维护一个zookeeper实例
    //private ZooKeeper zooKeeper;

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
        this.appName = appName;
        return this;
    }

    /**
     * 用来配置一个注册中心
     * @param registryConfig 注册中心
     * @return this 当前实例
     */
    public LjwrpcBootstrap registry(RegistryConfig registryConfig) {
        // 这里维护一个zookeeper实例，但是如果这样写就会将zookeeper和当前工程耦合
        // 我们其实更希望以后可以扩展更多种不同的实现

        // 尝试使用 registryConfig 获取一个注册中心，有点工厂设计模式的意思
        this.registry = registryConfig.getRegistry();
        return this;
    }

    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig 协议的封装
     * @return this 当前实例
     */
    public LjwrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
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
        // 我们抽象了注册中心的概念，使用注册中心的一个实现完成注册
        // 有人会想，此时此刻难道不是强耦合了吗？
        registry.registry(service);
        return this;
    }

    /**
     * 批量发布服务
     * @param services 封装的需要发布的服务的集合
     * @return this 当前实例
     */
    public LjwrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
            this.publish(service);
        }
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {
        try {
            Thread.sleep(1000000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * --------------------------------------服务调用方的相关api------------------------------------------
     */

    public LjwrpcBootstrap reference(ReferenceConfig<?> reference) {

        // 在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1.reference需要一个注册中心
        reference.setRegistry(registry);
        return this;
    }
}
