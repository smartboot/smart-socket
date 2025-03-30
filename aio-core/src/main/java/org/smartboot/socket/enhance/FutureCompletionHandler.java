/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: FutureCompletionHandler.java
 * Date: 2021-07-29
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 一个同时实现了CompletionHandler和Future接口的工具类，用于异步操作的完成处理和Future模式支持。
 * <p>
 * 该类提供了以下功能：
 * 1. 作为CompletionHandler处理异步操作的完成和失败回调
 * 2. 作为Future提供异步操作的结果获取、取消和状态查询
 * 3. 支持带超时的异步操作结果获取
 * </p>
 * @param <V> 异步操作的结果类型
 * @param <A> 异步操作的附加参数类型
 */
public final class FutureCompletionHandler<V, A> implements CompletionHandler<V, A>, Future<V> {
    /** 异步操作的执行结果 */
    private V result;
    /** 标记异步操作是否已完成（成功完成、失败或被取消） */
    private boolean done = false;
    /** 标记异步操作是否被取消 */
    private boolean cancel = false;
    /** 异步操作执行过程中发生的异常 */
    private Throwable exception;

    /**
     * 异步操作成功完成时的回调方法
     * @param result 异步操作的执行结果
     * @param selectionKey 异步操作的附加参数
     */
    @Override
    public void completed(V result, A selectionKey) {
        this.result = result;
        done = true;
        synchronized (this) {
            this.notify();
        }
    }

    /**
     * 异步操作失败时的回调方法
     * @param exc 异步操作执行过程中发生的异常
     * @param attachment 异步操作的附加参数
     */
    @Override
    public void failed(Throwable exc, A attachment) {
        exception = exc;
        done = true;
    }

    /**
     * 尝试取消异步操作的执行
     * @param mayInterruptIfRunning 是否允许中断正在执行的操作（在此实现中该参数不起作用）
     * @return 如果操作成功取消返回true，如果操作已完成或已被取消返回false
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (done || cancel) {
            return false;
        }
        cancel = true;
        done = true;
        synchronized (this) {
            notify();
        }
        return true;
    }

    /**
     * 检查异步操作是否被取消
     * @return 如果操作已被取消返回true，否则返回false
     */
    @Override
    public boolean isCancelled() {
        return cancel;
    }

    /**
     * 检查异步操作是否已完成
     * @return 如果操作已完成（包括成功完成、失败或被取消）返回true，否则返回false
     */
    @Override
    public boolean isDone() {
        return done;
    }

    /**
     * 获取异步操作的执行结果，如果操作未完成则阻塞等待
     * @return 异步操作的执行结果
     * @throws InterruptedException 等待过程中线程被中断
     * @throws ExecutionException 异步操作执行过程中发生异常
     */
    @Override
    public synchronized V get() throws InterruptedException, ExecutionException {
        if (done) {
            if (exception != null) {
                throw new ExecutionException(exception);
            }
            return result;
        } else {
            wait();
        }
        return get();
    }

    /**
     * 在指定的超时时间内获取异步操作的执行结果，如果操作未完成则阻塞等待
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 异步操作的执行结果
     * @throws InterruptedException 等待过程中线程被中断
     * @throws ExecutionException 异步操作执行过程中发生异常
     * @throws TimeoutException 超过指定时间仍未获得结果
     */
    @Override
    public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (done) {
            return get();
        } else {
            wait(unit.toMillis(timeout));
        }
        if (done) {
            return get();
        }
        throw new TimeoutException();
    }

}
