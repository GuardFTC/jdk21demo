package com.ftc.jdk21demo.virtual_thread;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: 冯铁城 [17615007230@163.com]
 * @date: 2025-05-28 17:42:46
 * @describe: 虚拟线程基础使用测试
 */
public class BaseUseTest {

    @Test
    void testVirtualThread() throws InterruptedException {

        //1.直接启动虚拟线程
        Thread.startVirtualThread(() -> {
            System.out.println("当前线程是否为虚拟线程:" + Thread.currentThread().isVirtual());
            System.out.println(Thread.currentThread().getName() + ":hello world!");
        }).join();

        //2.直接启动虚拟线程
        Thread.ofVirtual().name("hello virtual thread").start(() -> {
            System.out.println("当前线程是否为虚拟线程:" + Thread.currentThread().isVirtual());
            System.out.println(Thread.currentThread().getName() + ":hello world!");
        });

        //3.延迟启动虚拟线程
        Thread virtualThread = Thread.ofVirtual().name("hello virtual thread").unstarted(() -> {
            System.out.println("当前线程是否为虚拟线程:" + Thread.currentThread().isVirtual());
            System.out.println(Thread.currentThread().getName() + ":hello world!");
        });

        //4.等待虚拟线程结束
        virtualThread.start();
        virtualThread.join();
    }

    @Test
    void testVirtualThreadPool() throws InterruptedException {

        //1.创建虚拟线程池
        try (ExecutorService virtualExecutors = Executors.newVirtualThreadPerTaskExecutor()) {

            //2.基于虚拟线程池执行任务
            virtualExecutors.submit(() -> {
                System.out.println("当前线程是否为虚拟线程:" + Thread.currentThread().isVirtual());
                System.out.println(Thread.currentThread().getName() + ":hello world!");
            });

            //3.等待任务执行结束
            virtualExecutors.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
