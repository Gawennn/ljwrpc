package com.ljw;

import com.ljw.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Component
@Slf4j
public class LjwrpcStarter implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        Thread.sleep(5000);
        log.info("ljwrpc 开始启动...");
        LjwrpcBootstrap.getInstance()
                .application("first-ljwrpc-provider")
                // 配置注册中心
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("jdk")
                // 扫包批量发布
                .scan("com.ljw.impl")
                // 启动服务
                .start();
    }
}
