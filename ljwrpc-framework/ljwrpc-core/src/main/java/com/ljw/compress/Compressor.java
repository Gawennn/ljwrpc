package com.ljw.compress;

/**
 * @author 刘家雯
 * @version 1.0
 */
public interface Compressor {

    /**
     * 对字节数组进行压缩
     * @param bytes 待压缩的字节数组
     * @return 压缩后的字节数组
     */
    byte[] compress(byte[] bytes);

    /**
     * 对字节数组进行解压缩
     * @param bytes 待解压缩的字节数组
     * @return 解压缩后的字节数组
     */
    byte[] decompress(byte[] bytes);
}
