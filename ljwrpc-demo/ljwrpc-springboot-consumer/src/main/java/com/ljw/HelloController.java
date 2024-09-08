package com.ljw;

import com.ljw.annotation.LjwrpcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 刘家雯
 * @version 1.0
 */
@RestController
public class HelloController {

    // 需要注入一个代理对象
    @LjwrpcService
    private HelloLjwrpc helloLjwrpc;

    @GetMapping("hello")
    public String hello(){
        return helloLjwrpc.sayHi("hi provider");
    }
}
