package com.ljw.proxy.serialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 刘家雯
 * @version 1.0
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SerializerWrapper {

    private byte code;
    private String type;
    private Serializer serializer;
}
