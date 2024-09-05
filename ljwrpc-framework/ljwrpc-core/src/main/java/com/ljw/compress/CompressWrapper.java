package com.ljw.compress;

import com.ljw.serialize.Serializer;
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
public class CompressWrapper {

    private byte code;
    private String type;
    private Compressor compressor;
}
