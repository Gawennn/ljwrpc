package com.ljw;

import com.ljw.annotation.TryTimes;

/**
 * @author 刘家雯
 * @version 1.0
 */
public interface HelloLjwrpc {

    /**
     * 通用接口，server和client都需要依赖
     * @param msg 发送的具体消息
     * @return 返回的结果
     */
    @TryTimes(tryTimes = 3, intervalTime = 3000)
    String sayHi(String msg);
}
