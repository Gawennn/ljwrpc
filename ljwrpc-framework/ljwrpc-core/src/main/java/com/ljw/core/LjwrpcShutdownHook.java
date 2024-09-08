package com.ljw.core;

import java.util.concurrent.TimeUnit;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class LjwrpcShutdownHook extends Thread {

    @Override
    public void run() {
        // 1.打开挡板 (boolean)需要线程安全
        ShutDownHolder.BAFFLE.set(true);

        // 2.等待计数器归零（正常的请求处理结束） AtomicInteger
        // 等待归零，继续执行  ，最多等十秒
        long start = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (ShutDownHolder.REQUEST_COUNTER.sum() == 0L
                    || System.currentTimeMillis() - start > 10000) {
                break;
            }
        }

    }
}
