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
    /**
     * Worker线程池
     */
    private ThreadPoolExecutor workerThreadPool;
    /**
     * 读回调资源信号量
     */
    private Semaphore semaphore;
    /**
     * 递归线程标识
     */
    private ThreadLocal<ReadCompletionHandler> recursionThreadLocal = new ThreadLocal<>();

    public ReadCompletionHandler() {
    }

    public ReadCompletionHandler(ThreadPoolExecutor workerThreadPool, Semaphore semaphore) {
        this.workerThreadPool = workerThreadPool;
        this.semaphore = semaphore;
    }

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
        //未启用Worker线程池或者被递归回调complated直接执行completed0
        if (workerThreadPool == null || recursionThreadLocal.get() != null) {
            runTask();
            completed0(result, aioSession);
            return;
        }

        //Boss线程不处理读回调，或者Boss线程中的读信号量不足
        if (semaphore == null || !semaphore.tryAcquire()) {
            workerThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    if (recursionThreadLocal.get() != null) {
                        completed0(result, aioSession);
                    } else {
                        recursionThreadLocal.set(ReadCompletionHandler.this);
                        completed0(result, aioSession);
                        recursionThreadLocal.remove();
                    }
                }
            });
            return;
        }
        try {
            recursionThreadLocal.set(this);
            runTask();
            completed0(result, aioSession);
            recursionThreadLocal.remove();
//            executeTask();
        } finally {
            semaphore.release();
        }
    }

    /**
     * 执行异步队列中的任务
     */
    private void executeTask() {
        if (workerThreadPool != null) {
            int count = RUN_LIMIT;
            BlockingQueue<Runnable> taskQueue = workerThreadPool.getQueue();
            Runnable runnable = null;
            while (count-- > 0 && (runnable = taskQueue.poll()) != null) {
                runnable.run();
            }
        }
    }

    /**
     * 执行异步队列中的任务
     */
    private void runTask() {
        if (workerThreadPool == null) {
            return;
        }
        Runnable runnable = workerThreadPool.getQueue().poll();
        if (runnable != null) {
            runnable.run();
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