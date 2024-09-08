package com.ljw.exceptions;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class LoadBalancerException extends RuntimeException{

    public LoadBalancerException(String message) {
        super(message);
    }

    public LoadBalancerException() {
    }
}
