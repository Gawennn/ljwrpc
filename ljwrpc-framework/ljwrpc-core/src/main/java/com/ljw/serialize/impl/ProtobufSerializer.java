package com.ljw.serialize.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.ljw.serialize.protobuf.LjwrpcMessage;
import com.ljw.exceptions.SerializeException;
import com.ljw.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 数据体量小
 * 先写一个.proto文件，然后通过protoc编译生成对应的包含LjwRequest和LjwResponse的类，里面就包含了parseFrom方法
 *
 * @author 刘家雯
 * @version 1.0
 * @Date 2024/10/22
 */
@Slf4j
public class ProtobufSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        if (object == null) return null;

        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            // 先判断对象类型是不是protobuf序列化生成的类
            if (object instanceof com.google.protobuf.GeneratedMessageV3) {
                byte[] result = ((com.google.protobuf.GeneratedMessageV3) object).toByteArray();
                if (log.isDebugEnabled()) {
                    log.debug("对象【{}】已经完成了序列化操作，序列化后的字节数为【{}】", object, result.length);
                }
                return result;
            } else {
                throw new SerializeException("对象类型不支持 Protobuf 序列化");
            }
        } catch (IOException e) {
            log.error("使用 Protobuf 进行序列化对象【{}】时发生异常.", object);
            throw new SerializeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) return null;

        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ) {
            // 判断目标类是否为 protobuf 序列化生成的类
            if (com.google.protobuf.GeneratedMessageV3.class.isAssignableFrom(clazz)) {
                // 调用parseProtobuf反序列化
                return parseProtobuf(bytes, clazz);
            } else {
                throw new SerializeException("对象类型不支持 Protobuf 反序列化");
            }
        } catch (Exception e) {
            log.error("使用 Protobuf 进行反序列化对象【{}】时发生异常.", clazz);
            throw new SerializeException(e);
        }
    }

    private <T> T parseProtobuf(byte[] bytes, Class<T> clazz) throws InvalidProtocolBufferException {
        if (clazz == com.ljw.transport.LjwrpcRequest.class) {
            return (T) LjwrpcMessage.LjwrpcRequest.parseFrom(bytes);
        } else if (clazz == com.ljw.transport.LjwrpcResponse.class) {
            return (T) LjwrpcMessage.LjwrpcResponse.parseFrom(bytes);
        } else {
            throw new SerializeException("不支持的 Protobuf 类型：" + clazz.getName());
        }
    }
}
