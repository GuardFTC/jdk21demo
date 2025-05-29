package com.ftc.jdk21demo.virtual_thread;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;

/**
 * @author: 冯铁城 [17615007230@163.com]
 * @date: 2025-05-29 15:03:39
 * @describe: ScopedValue测试类
 */
@Slf4j
public class ScopedValueTest {

    private static final MockService SERVICE = new MockService();

    @Test
    void testScopedValue() {

        //1.ftc
        String username = "ftc";
        String userInfoAndAddress = SERVICE.getUserInfoAndAddress(username);
        Assert.isTrue("userInfo:ftc-userAddress:ftc".equals(userInfoAndAddress));

        //2.zyl
        username = "zyl";
        userInfoAndAddress = SERVICE.getUserInfoAndAddress(username);
        Assert.isTrue("userInfo:zyl-userAddress:zyl".equals(userInfoAndAddress));
    }

    /**
     * Scoped上下文统一管理
     */
    private static class ScopedContext {

        /**
         * 名称上下文
         */
        private static final ScopedValue<String> NAME = ScopedValue.newInstance();
    }

    /**
     * 模拟服务
     */
    private static class MockService {

        private final MockLogger logger = new MockLogger();

        /**
         * 获取用户信息以及用户地址
         *
         * @param name 用户名
         * @return 用户信息以及用户地址
         */
        public String getUserInfoAndAddress(String name) {

            //1.将name值绑定到ScopedValue中
            try {
                return ScopedValue.where(ScopedContext.NAME, name).call(() -> {

                    //2.创建结构化并发作用域
                    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

                        //3.异步获取用户详情以及用户地址
                        Supplier<String> userInfoFork = scope.fork(() -> getUserInfo());
                        Supplier<String> userAddressFork = scope.fork(() -> getUserAddress());

                        //4.阻塞等待所有异步任务
                        scope.join();

                        //5.捕获第一个抛出的异常
                        scope.throwIfFailed();

                        //6.返回结果
                        return STR."\{userInfoFork.get()}-\{userAddressFork.get()}";
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 获取用户信息
         *
         * @return 用户信息
         */
        private String getUserInfo() {
            logger.log("获取用户信息");
            return STR."userInfo:\{ScopedContext.NAME.get()}";
        }

        /**
         * 获取用户地址
         *
         * @return 用户地址
         */
        private String getUserAddress() {
            logger.log("获取用户地址");
            return STR."userAddress:\{ScopedContext.NAME.get()}";
        }
    }

    /**
     * 模拟日志打印器
     */
    private static class MockLogger {

        /**
         * 打印日志
         *
         * @param msg 日志信息
         */
        public void log(String msg) {
            log.info("用户:{}-{}", ScopedContext.NAME.get(), msg);
        }
    }
}
