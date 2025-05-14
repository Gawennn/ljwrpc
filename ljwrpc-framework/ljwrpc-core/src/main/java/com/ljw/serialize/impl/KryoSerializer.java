package com.ljw.serialize.impl;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.ljw.exceptions.SerializeException;
import com.ljw.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Kryo 序列化器
 *
 * @author 刘家雯
 * @version 1.0
 * @Date 2024/10/22
 */
@Slf4j
public class KryoSerializer implements Serializer {

    // kryo 是线程不安全的，可以用ThreadLocal或ConcurrentHashMap
    private static final ThreadLocal<Kryo> kryoThreadLocal = new ThreadLocal<>();
    Kryo kryo = new Kryo();

    @Override
    public byte[] serialize(Object object) {
        if (object == null) return null;
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // 先创建一个Output对象，然后使用writeObject将Object对象写入output中，再调用getBytes即可获得对象的字节数组。
                Output output = new Output(baos)) // 创建Kryo输出流
        {
            Kryo kryo1 = kryoThreadLocal.get();
            kryo1.writeObject(output, object); // 进行序列化，也就是Kryo的序列化主要方法
            output.flush();

            byte[] result = baos.toByteArray();
            if (log.isDebugEnabled()) {
                log.debug("对象【{}】已经完成了序列化操作，序列化后的字节数为【{}】", object, result.length);
            }
            kryoThreadLocal.remove();
            return result;
        } catch (IOException e) {
            log.error("序列化对象【{}】时发生异常。", object);
            throw new SerializeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) return null;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             Input input = new Input(bais)) // 创建Kryo输入流
        {
            T t = kryo.readObject(input, clazz); // 核心方法，使用kryo进行反序列化
            if(log.isDebugEnabled()) {
                log.debug("类【{}】已经使用Kryo完成了反序列化操作。", clazz);
            }

            return t;
        } catch (IOException e) {
            log.error("使用Kryo进行反序列化对象【{}】时发生异常.", clazz);
            throw new SerializeException(e);
        }
    }
}
