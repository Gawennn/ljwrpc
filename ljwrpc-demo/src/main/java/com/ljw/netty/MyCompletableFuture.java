package com.ljw.netty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class MyCompletableFuture {

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {

        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();

        // 开启一个子线程
        new Thread(
            () -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                int i = 8;

                completableFuture.complete(i);
            }
        ).start();

        // 在主线程中排队去获取子线程这个i
        // get方法是一个阻塞的方法
        Integer integer = completableFuture.get(3, TimeUnit.SECONDS);
        System.out.println(integer);
    }
}
