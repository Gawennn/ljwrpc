package com.ljw.proxy.serialize;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class SerializeUtil {

    public static byte[] serialize(Object object){
        // 针对不同的消息类型需要做不同的处理，心跳的请求，没有payload
        if (object == null) {
            return null;
        }

        // 希望可以通过一些设计模式，面向对象的编程，让我们可以配置修改 序列化和压缩 的方式
        // 对象怎么变成一个字节数组   序列化   压缩
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);
            objectOutputStream.writeObject(object);
            return baos.toByteArray();
            // 压缩

        } catch (IOException e) {
            log.error("序列化时出现异常");
            throw new RuntimeException(e);
        }
    }
}
