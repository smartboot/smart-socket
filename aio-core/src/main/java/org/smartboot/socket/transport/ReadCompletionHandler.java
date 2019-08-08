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
import org.smartboot.socket.list.Node;
import org.smartboot.socket.list.RingBuffer;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.Semaphore;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
class ReadCompletionHandler<T> implements CompletionHandler<Integer, AioSession<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadCompletionHandler.class);
    /**
     * 读回调资源信号量
     */
    private Semaphore semaphore;
    /**
     * 递归线程标识
     */
    private ThreadLocal<CompletionHandler> recursionThreadLocal = null;

    private RingBuffer ringBuffer;

    private Semaphore readSemaphore;

    public ReadCompletionHandler() {
    }

    public ReadCompletionHandler(final RingBuffer ringBuffer, final ThreadLocal<CompletionHandler> recursionThreadLocal, Semaphore semaphore) {
        this.semaphore = semaphore;
        int avail = semaphore.availablePermits();
        this.readSemaphore = new Semaphore(avail > 1 ? avail - 1 : 1);
        this.recursionThreadLocal = recursionThreadLocal;
        LOGGER.info("semaphore:{} ,readSemaphore:{}", avail, readSemaphore.availablePermits());
        this.ringBuffer = ringBuffer;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Node node = ringBuffer.take();
                        AioSession aioSession = node.getSession();
                        int size = node.getSize();
                        ringBuffer.resetNode(node);
                        completed0(size, aioSession);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }, "smart-socket:BossDefendThread").start();
    }

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
        //未启用Worker线程池或者被递归回调complated直接执行completed0
        if (recursionThreadLocal == null || recursionThreadLocal.get() != null) {
            completed0(result, aioSession);
            runTask();
            return;
        }

        //Boss线程不处理读回调，或者Boss线程中的读信号量不足
        if (semaphore == null || !semaphore.tryAcquire()) {
            try {
                ringBuffer.put(aioSession, result);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            recursionThreadLocal.set(this);
            completed0(result, aioSession);
            runTask();
        } finally {
            recursionThreadLocal.remove();
            semaphore.release();
        }
    }

    /**
     * 执行异步队列中的任务
     */
    void runTask() {
        if (ringBuffer == null) {
            return;
        }
        Node node = ringBuffer.poll();
        if (node == null) {
            return;
        }
        AioSession aioSession = node.getSession();
        int size = node.getSize();
        ringBuffer.resetNode(node);
        completed0(size, aioSession);

//        if (readSemaphore.tryAcquire()) {
//            try {
//                while ((node = ringBuffer.poll()) != null) {
//                    aioSession = node.getSession();
//                    size = node.getSize();
//                    ringBuffer.resetNode(node);
//                    completed0(size, aioSession);
//                }
//            } finally {
//                readSemaphore.release();
//            }
//        }
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