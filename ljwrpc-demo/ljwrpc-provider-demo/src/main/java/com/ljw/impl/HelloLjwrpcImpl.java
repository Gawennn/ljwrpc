package com.ljw.impl;

import com.ljw.HelloLjwrpc;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class HelloLjwrpcImpl implements HelloLjwrpc {

    @Override
    public String sayHi(String msg) {
        return "hi consumer" + msg;
    }
}
