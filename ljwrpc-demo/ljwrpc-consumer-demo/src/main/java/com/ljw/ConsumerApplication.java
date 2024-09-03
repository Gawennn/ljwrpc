package com.ljw;

import com.ljw.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class ConsumerApplication {

    public static void main(String[] args) {

        // 想尽一切办法获取代理对象，使用ReferenceConfig进行封装
        // 因为ReferenceConfig中一定有生成代理的模版方法, get()
        ReferenceConfig<HelloLjwrpc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloLjwrpc.class);

        // 代理做了些什么
        // 1.连接注册中心
        // 2.拉取服务列表
        // 3.选择一个服务并连接服务
        // 4.发送请求，携带一些信息（接口名，参数列表，方法名字），获得结果
        LjwrpcBootstrap.getInstance()
                .application("first-ljwrpc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .reference(reference);

        // 获取一个代理对象
        HelloLjwrpc helloLjwrpc = reference.get();
        String sayHi = helloLjwrpc.sayHi("你好");
        log.info("sayHi-->{}", sayHi);
    }
}
