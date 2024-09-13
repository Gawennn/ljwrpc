package com.ljw.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 挡板，用来开启或关闭 一个服务的
 *
 * @author 刘家雯
 * @version 1.0
 */
public class ShutDownHolder {

    // 用来标记请求的挡板
    public static AtomicBoolean BAFFLE = new AtomicBoolean(false);

    // 用于请求的计数器
    public static LongAdder REQUEST_COUNTER = new LongAdder();
}
