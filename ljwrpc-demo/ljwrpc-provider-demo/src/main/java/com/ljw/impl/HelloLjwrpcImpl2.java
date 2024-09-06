package com.ljw.impl;

import com.ljw.HelloLjwrpc;
import com.ljw.HelloLjwrpc2;
import com.ljw.annotation.LjwrpcApi;

/**
 * @author 刘家雯
 * @version 1.0
 */
@LjwrpcApi
public class HelloLjwrpcImpl2 implements HelloLjwrpc2 {

    @Override
    public String sayHi(String msg) {
        return "hi consumer" + msg;
    }
}
