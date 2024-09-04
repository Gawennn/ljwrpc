package com.ljw.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务提供方回复的响应
 *
 * @author 刘家雯
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LjwrpcResponse {

    // 请求的id
    private Long requestId;

    // 请求的类型
    private byte requestType;

    // 压缩的类型
    private byte compressType;

    // 序列化的方式
    private byte serializeType;

    // 响应码类型 1 成功, 2 异常
    private byte code;

    // 具体的消息体
    private Object body;
}
