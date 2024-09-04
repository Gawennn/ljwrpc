package com.ljw.exceptions;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class SerializeException extends RuntimeException{

    public SerializeException() {
    }

    public SerializeException(String message) {
        super(message);
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }
}
