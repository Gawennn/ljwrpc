package com.ljw.exceptions;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class NetworkException extends RuntimeException{

    public NetworkException() {
    }

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(Throwable cause) {
        super(cause);
    }
}
