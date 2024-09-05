package com.ljw.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务调用方发起的请求内容
 *
 * @author 刘家雯
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LjwrpcRequest {

    // 请求的id
    private Long requestId;

    // 请求的类型
    private byte requestType;

    // 压缩的类型
    private byte compressType;

    // 序列化的方式
    private byte serializeType;

    // 时间戳
    private long timeStamp;

    // 具体的消息体
    private RequestPayload requestPayload;
}
