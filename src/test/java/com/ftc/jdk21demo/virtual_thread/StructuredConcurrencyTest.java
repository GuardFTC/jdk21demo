package com.ftc.jdk21demo.virtual_thread;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author: 冯铁城 [fengtiecheng@pwrd.com]
 * @date: 2025-05-28 20:11:18
 * @describe: 结构化并发测试
 */
public class StructuredConcurrencyTest {

    @Test
    void testStructuredConcurrencyShowdownOnFailureNoException() {

        //1.开启结构化并发，任意一个异步任务失败，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            //2.异步获取数据
            Supplier<List<String>> fork = scope.fork(() -> mockReadData(false, 0, StrUtil.EMPTY));

            //3.等待任务执行完成
            scope.join();

            //4.断言异步任务结果
            assertAsyncTaskResult(fork.get());
        } catch (Exception e) {
            Assert.isNull(e);
        }
    }

    @Test
    void testStructuredConcurrencyShowdownOnFailureThrowException() {

        //1.开启结构化并发，任意一个异步任务失败，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            //2.异步获取数据
            Supplier<List<String>> fork = scope.fork(() -> mockReadData(true, 0, StrUtil.EMPTY));

            //3.等待任务执行完成
            scope.join();

            //4.断言异步任务结果(当前任务永远不会走到这里)
            assertAsyncTaskResult(fork.get());
        } catch (Exception e) {
            Assert.isTrue(ObjectUtil.isNotNull(e));
            String expectMessage = "Subtask not completed or did not complete successfully";
            Assert.isTrue(expectMessage.equals(e.getMessage()));
        }
    }

    @Test
    void testStructuredConcurrencyShowdownOnFailureThrowIfFailed() {

        //1.开启结构化并发，任意一个异步任务失败，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            //2.创建抛出异常异步任务
            Supplier<List<String>> fork1 = scope.fork(() -> {
                System.out.println("task1 start running");
                List<String> result = mockReadData(true, 400, StrUtil.EMPTY);
                System.out.println("task1 is end");
                return result;
            });

            //3.创建正常异步任务
            Supplier<List<String>> fork2 = scope.fork(() -> {
                System.out.println("task2 start running");
                List<String> result = mockReadData(false, 100, StrUtil.EMPTY);
                System.out.println("task2 is end");
                return result;
            });

            //4.等待任务执行完成
            scope.join();

            //5.任意一个任务异常，则结束流程并抛出第一个异常
            scope.throwIfFailed();

            //6.断言异步任务结果(当前任务永远不会走到这里)
            assertAsyncTaskResult(fork1.get());
            assertAsyncTaskResult(fork2.get());
        } catch (Exception e) {
            Assert.isTrue(ObjectUtil.isNotNull(e));
            String expectMessage = "java.lang.RuntimeException: 模拟读取数据失败";
            Assert.isTrue(expectMessage.equals(e.getMessage()));
        }
    }

    @Test
    void testStructuredConcurrencyShowdownOnFailureThrowIfFailedMoreException() {

        //1.开启结构化并发，任意一个异步任务失败，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            //2.创建抛出异常异步任务
            Supplier<List<String>> fork1 = scope.fork(() -> {
                System.out.println("task1 start running");
                List<String> result = mockReadData(true, 100, "task1");
                System.out.println("task1 is end");
                return result;
            });

            //3.创建抛出抑制性异常的异步任务
            StructuredTaskScope.Subtask<Object> fork3 = scope.fork(() -> {
                System.out.println("task2 start running");
                List<String> result = mockReadData(false, 300, "task3");
                System.out.println("task2 is end");
                return result;
            });

            //4.等待任务执行完成
            scope.join();

            //5.任意一个任务异常，则结束流程并抛出第一个异常
            scope.throwIfFailed();

            //6.断言异步任务结果(当前任务永远不会走到这里)
            assertAsyncTaskResult(fork1.get());
        } catch (Exception e) {

            //7.校验第一个抛出的异常信息(主异常)
            Assert.isTrue(ObjectUtil.isNotNull(e));
            String expectMessage = "模拟读取数据失败";
            Assert.isTrue(e.getMessage().contains(expectMessage));

            //8.获取被抑制的异常
            //抑制异常，指的是，因为其他异步任务失败，因此被scope终止，所引发的异常
            //当结构化并发中某个任务先失败，scope 会主动取消其他还未完成的任务（通过 Thread.interrupt()），
            //这些被中断的任务在退出时如果也抛出了异常（比如 InterruptedException）
            //它们的异常就不会作为主异常抛出，而是作为被“抑制”的异常附加到主异常上。
            Throwable[] suppressedExceptions = e.getSuppressed();
            for (Throwable suppressedException : suppressedExceptions) {
                System.out.println(STR."被抑制异常信息:\{suppressedException.getMessage()}");
            }
        }
    }

    @Test
    void testStructuredConcurrencyShowdownOnSuccessNoException() {

        //1.开启结构化并发，任意一个异步任务成功，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<>()) {

            //2.异步获取数据
            Supplier<List<String>> fork = scope.fork(() -> mockReadData(false, 0, StrUtil.EMPTY));

            //3.等待任务执行完成
            scope.join();

            //4.断言异步任务结果
            assertAsyncTaskResult(fork.get());
        } catch (Exception e) {
            Assert.isNull(e);
        }
    }

    @Test
    void testStructuredConcurrencyShowdownOnSuccessThrowException() {

        //1.开启结构化并发，任意一个异步任务成功，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            //2.异步获取数据
            Supplier<List<String>> fork = scope.fork(() -> mockReadData(true, 0, StrUtil.EMPTY));

            //3.等待任务执行完成
            scope.join();

            //4.断言异步任务结果(当前任务永远不会走到这里)
            assertAsyncTaskResult(fork.get());
        } catch (Exception e) {
            Assert.isTrue(ObjectUtil.isNotNull(e));
            String expectMessage = "Subtask not completed or did not complete successfully";
            Assert.isTrue(expectMessage.equals(e.getMessage()));
        }
    }

    @Test
    void testStructuredConcurrencyShowdownOnSuccessOneSuccess() {

        //1.开启结构化并发，任意一个异步任务成功，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<>()) {

            //2.创建快速异步任务
            scope.fork(() -> {
                System.out.println("task1 start running");
                List<String> result = mockReadData(false, 100, "task1");
                System.out.println("task1 is end");
                return result;
            });

            //3.创建慢速异步任务
            scope.fork(() -> {
                System.out.println("task2 start running");
                List<String> result = mockReadData(false, 300, "task2");
                System.out.println("task2 is end");
                return result;
            });

            //3.等待任务执行完成
            scope.join();

            //4.获取第一个执行完成的异步任务结果
            List<String> result = (List<String>) scope.result();

            //5.断言异步任务结果
            assertAsyncTaskResult(result);
        } catch (Exception e) {
            Assert.isNull(e);
        }
    }

    @Test
    void testStructuredConcurrencyShowdownOnSuccessOneSuccessOneFailed() {

        //1.开启结构化并发，任意一个异步任务成功，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<>()) {

            //2.创建快速异步任务
            scope.fork(() -> {
                System.out.println("task1 start running");
                List<String> result = mockReadData(true, 100, "task1");
                System.out.println("task1 is end");
                return result;
            });

            //3.创建慢速异步任务
            scope.fork(() -> {
                System.out.println("task2 start running");
                List<String> result = mockReadData(false, 300, "task2");
                System.out.println("task2 is end");
                return result;
            });

            //3.等待任务执行完成
            scope.join();

            //4.获取第一个执行完成的异步任务结果
            List<String> result = (List<String>) scope.result();

            //5.断言异步任务结果
            assertAsyncTaskResult(result);
        } catch (Exception e) {
            Assert.isNull(e);
        }
    }

    @Test
    void testStructuredConcurrencyShowdownOnSuccessAllFailed() {

        //1.开启结构化并发，任意一个异步任务成功，则立即关闭结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<>()) {

            //2.创建快速异步任务
            scope.fork(() -> {
                System.out.println("task1 start running");
                List<String> result = mockReadData(true, 100, "task1");
                System.out.println("task1 is end");
                return result;
            });

            //3.创建慢速异步任务
            scope.fork(() -> {
                System.out.println("task2 start running");
                List<String> result = mockReadData(true, 300, "task2");
                System.out.println("task2 is end");
                return result;
            });

            //3.等待任务执行完成
            scope.join();

            //4.获取第一个执行完成的异步任务结果
            List<String> result = (List<String>) scope.result();

            //5.断言异步任务结果
            assertAsyncTaskResult(result);
        } catch (Exception e) {
            Assert.isTrue(ObjectUtil.isNotNull(e));
            String expectMessage = "模拟读取数据失败";
            Assert.isTrue(e.getMessage().contains(expectMessage));
        }
    }

    /**
     * 模拟读取数据
     *
     * @param isThrowException  是否抛出异常
     * @param timeOut           方法模拟耗时/ms
     * @param errorMessageTitle 异常信息
     * @return 数据列表
     */
    private List<String> mockReadData(boolean isThrowException, long timeOut, String errorMessageTitle) throws InterruptedException {

        //1.模拟耗时
        if (timeOut > 0) {
            TimeUnit.MILLISECONDS.sleep(timeOut);
        }

        //2.如果需要抛出异常，则抛出对应异常
        if (isThrowException) {
            throw new RuntimeException(errorMessageTitle + "模拟读取数据失败");
        }

        //3.返回数据
        return List.of("1", "2", "3");
    }

    /**
     * 模拟写入数据
     *
     * @param data 数据
     */
    private void mockWriteData(String data) {
        System.out.println(STR."数据写入成功\{data}");
    }

    /**
     * 断言异步任务结果
     *
     * @param data 待验证数据
     */
    private static void assertAsyncTaskResult(List<String> data) {
        Assert.isTrue(data.size() == 3);
        Assert.isTrue(data.get(0).equals("1"));
        Assert.isTrue(data.get(1).equals("2"));
        Assert.isTrue(data.get(2).equals("3"));
    }
}
