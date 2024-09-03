package com.ljw.exceptions;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class DiscoveryException extends RuntimeException{

    public DiscoveryException() {
    }

    public DiscoveryException(String message) {
        super(message);
    }

    public DiscoveryException(Throwable cause) {
        super(cause);
    }
}
