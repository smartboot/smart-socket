/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ReadCompletionHandler.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
class ReadCompletionHandler<T> implements CompletionHandler<Integer, AioSession<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadCompletionHandler.class);
    private static final int RUN_LIMIT = 16;
    private ThreadPoolExecutor workerThreadPool;
    /**
     * Worker线程池中的任务队列
     */
    private BlockingQueue<Runnable> taskQueue;
    /**
     * 读回调资源信号量
     */
    private Semaphore semaphore;
    /**
     * 递归线程标识
     */
    private ThreadLocal<ReadCompletionHandler> RECURSION_THREAD_LOCAL = new ThreadLocal<>();

    public ReadCompletionHandler() {
    }

    public ReadCompletionHandler(ThreadPoolExecutor workerThreadPool, Semaphore semaphore) {
        this.workerThreadPool = workerThreadPool;
        this.taskQueue = workerThreadPool.getQueue();
        this.semaphore = semaphore;
    }

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
        //未启用Worker线程池或者被递归回调complated直接执行completed0
        if (workerThreadPool == null || RECURSION_THREAD_LOCAL.get() != null) {
            completed0(result, aioSession);
            return;
        }

        //Boss线程不处理读回调，或者Boss线程中的读信号量不足
        if (semaphore == null || !semaphore.tryAcquire()) {
            workerThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    RECURSION_THREAD_LOCAL.set(ReadCompletionHandler.this);
                    completed0(result, aioSession);
                    RECURSION_THREAD_LOCAL.remove();
                }
            });
            return;
        }
        try {
            RECURSION_THREAD_LOCAL.set(this);
            completed0(result, aioSession);
            RECURSION_THREAD_LOCAL.remove();
            executeTask();
        } finally {
            semaphore.release();
        }
    }

    /**
     * 执行异步队列中的任务
     */
    private void executeTask() {
        if (taskQueue != null) {
            int count = RUN_LIMIT;
            Runnable runnable = null;
            while (count-- > 0 && (runnable = taskQueue.poll()) != null) {
                runnable.run();
            }
        }
    }

    private void completed0(final Integer result, final AioSession<T> aioSession) {
        try {
            // 接收到的消息进行预处理
            NetMonitor<T> monitor = aioSession.getServerConfig().getMonitor();
            if (monitor != null) {
                monitor.readMonitor(aioSession, result);
            }
            aioSession.readFromChannel(result == -1);
        } catch (Exception e) {
            failed(e, aioSession);
        }
    }

    @Override
    public void failed(Throwable exc, AioSession<T> aioSession) {

        try {
            aioSession.getServerConfig().getProcessor().stateEvent(aioSession, StateMachineEnum.INPUT_EXCEPTION, exc);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        try {
            aioSession.close(false);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }
}