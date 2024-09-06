package com.ljw.impl;

import com.ljw.HelloLjwrpc;
import com.ljw.annotation.LjwrpcApi;

/**
 * @author 刘家雯
 * @version 1.0
 */
@LjwrpcApi(group = "primary")
public class HelloLjwrpcImpl implements HelloLjwrpc {

    @Override
    public String sayHi(String msg) {
        return "hi consumer" + msg;
    }
}
