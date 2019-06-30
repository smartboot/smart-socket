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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
class ReadCompletionHandler<T> implements CompletionHandler<Integer, AioSession<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadCompletionHandler.class);
    private static final int RUN_LIMIT = 16;
    private ExecutorService executorService;
    private BlockingQueue<Runnable> taskQueue;
    private Semaphore semaphore;

    public ReadCompletionHandler() {
    }

    public ReadCompletionHandler(ExecutorService executorService, BlockingQueue<Runnable> taskQueue, Semaphore semaphore) {
        this.executorService = executorService;
        this.taskQueue = taskQueue;
        this.semaphore = semaphore;
    }

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
        if (executorService == null || aioSession.recursion) {
            completed0(result, aioSession);
            return;
        }

        if (semaphore == null || !semaphore.tryAcquire()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    aioSession.recursion = true;
                    completed0(result, aioSession);
                    aioSession.recursion = false;
                }
            });
            return;
        }
        aioSession.recursion = true;
        try {
            completed0(result, aioSession);
            aioSession.recursion = false;
            if (taskQueue != null) {
                int count = RUN_LIMIT;
                Runnable runnable = null;
                while (count-- > 0 && (runnable = taskQueue.poll()) != null) {
                    runnable.run();
                }
            }
        } finally {
            semaphore.release();
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