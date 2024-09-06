package com.ljw.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectWrapper<T> {

    private Byte code;
    private String name;
    private T impl;
}
