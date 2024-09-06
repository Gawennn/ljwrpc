package com.ljw.exceptions;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class ResponseException extends RuntimeException{

    private byte code;
    private String msg;

    public ResponseException(byte code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
