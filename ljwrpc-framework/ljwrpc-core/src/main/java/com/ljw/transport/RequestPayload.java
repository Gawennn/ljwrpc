package com.ljw.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 他用来描述，请求调用方所请求的接口方法的描述
 *
 * @author 刘家雯
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestPayload implements Serializable {

    // 1.接口的名字  com.ljw.HelloLjwrpc
    private String interfaceName;

    // 2.方法的名字  sayHi
    private String methodName;

    // 3.参数列表，参数分为参数类型和具体参数
    // 参数类型用来确定重载方法
    // 具体的参数用来执行方法调用
    private Class<?>[] parametersType;  // {java.lang.String}
    private Object[] parameterValue;  // "你好"

    // 4.返回值的封装
    private Class<?> returnType;  // {java.lang.String}
}
