package com.ftc.jdk21demo.virtual_thread;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: 冯铁城 [fengtiecheng@pwrd.com]
 * @date: 2025-05-28 19:41:05
 * @describe: 虚拟线程池测试相关
 */
public class VirtualExecutorsTest {

    @Test
    public void testWithCompletableFuture() throws InterruptedException, ExecutionException {

        //1.创建虚拟线程池
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        //2.基于CompletableFuture创建任务,无返回值
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
            System.out.println("当前线程是否为虚拟线程:" + Thread.currentThread().isVirtual());
            System.out.println(Thread.currentThread().getName() + ":hello world!(无返回值)");
        }, virtualExecutor);

        //3.基于CompletableFuture创建任务,有返回值
        CompletableFuture<String> supplyCompletableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程是否为虚拟线程:" + Thread.currentThread().isVirtual());
            return Thread.currentThread().getName() + ":hello world!(有返回值)";
        }, virtualExecutor);

        //4.阻塞式等待异步任务执行完成
        CompletableFuture.allOf(voidCompletableFuture, supplyCompletableFuture).join();

        //5.输出有返回值异步任务的返回值
        System.out.println(supplyCompletableFuture.get());
    }
}
