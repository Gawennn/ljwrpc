package com.ljw.exceptions;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class CompressException extends RuntimeException{

    public CompressException() {
    }

    public CompressException(String message) {
        super(message);
    }

    public CompressException(Throwable cause) {
        super(cause);
    }
}
