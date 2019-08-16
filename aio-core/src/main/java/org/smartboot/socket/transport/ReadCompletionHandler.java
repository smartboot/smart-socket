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
import org.smartboot.socket.buffer.ring.RingBuffer;

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

    private RingBuffer<ReadEvent> ringBuffer;

    private Semaphore readSemaphore;

    private Object defendThreadLock = new Object();
    private volatile boolean defendThreadBlockFlag = false;

    public ReadCompletionHandler() {
    }

    public ReadCompletionHandler(final RingBuffer<ReadEvent> ringBuffer, final ThreadLocal<CompletionHandler> recursionThreadLocal, Semaphore semaphore) {
        this.semaphore = semaphore;
        int avail = semaphore.availablePermits();
        this.readSemaphore = new Semaphore(1);
        this.recursionThreadLocal = recursionThreadLocal;
        LOGGER.info("semaphore:{} ,readSemaphore:{}", avail, readSemaphore.availablePermits());
        this.ringBuffer = ringBuffer;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (defendThreadBlockFlag) {
                            synchronized (defendThreadLock) {
                                if (defendThreadBlockFlag) {
                                    defendThreadLock.wait();
                                }
                            }
                        }
                        int consumerIndex = ringBuffer.nextReadIndex();
                        ReadEvent readEvent = ringBuffer.get(consumerIndex);
                        AioSession aioSession = readEvent.getSession();
                        int size = readEvent.getReadSize();
                        ringBuffer.publishReadIndex(consumerIndex);
                        completed0(size, aioSession);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }, "smart-socket:BossDefendThread");
        t.setPriority(1);
        t.start();
    }

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
        //未启用Worker线程池或者被递归回调completed直接执行completed0
        if (recursionThreadLocal == null || recursionThreadLocal.get() != null) {
            completed0(result, aioSession);
            runTask();
            return;
        }

        //Boss线程不处理读回调，或者Boss线程中的读信号量不足
        if (semaphore == null || !semaphore.tryAcquire()) {
            try {
                int sequence = ringBuffer.nextWriteIndex();
                ReadEvent readEvent = ringBuffer.get(sequence);
                readEvent.setSession(aioSession);
                readEvent.setReadSize(result);
                ringBuffer.publishWriteIndex(sequence);
//                ringBuffer.put(aioSession, result);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            runAllTask();
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
    void runAllTask() {
        if (ringBuffer == null) {
            return;
        }
        if (readSemaphore.tryAcquire()) {
            try {
                defendThreadBlockFlag = true;
                int index = -1;
                ReadEvent readEvent;
                AioSession aioSession;
                int size;
                while ((index = ringBuffer.tryNextReadIndex()) >= 0) {
                    readEvent = ringBuffer.get(index);
                    aioSession = readEvent.getSession();
                    size = readEvent.getReadSize();
                    ringBuffer.publishReadIndex(index);
                    completed0(size, aioSession);
                }
                defendThreadBlockFlag = false;
                synchronized (defendThreadLock) {
                    defendThreadLock.notifyAll();
                }
            } finally {
                readSemaphore.release();
            }
        }
    }

    /**
     * 执行异步队列中的任务
     */
    void runTask() {
        if (ringBuffer == null) {
            return;
        }
        int index = ringBuffer.tryNextReadIndex();
        if (index < 0) {
            return;
        }
        ReadEvent readEvent = ringBuffer.get(index);
        AioSession aioSession = readEvent.getSession();
        int size = readEvent.getReadSize();
        ringBuffer.publishReadIndex(index);
        completed0(size, aioSession);

//        if (readSemaphore.tryAcquire()) {
//            try {
//                defendThreadBlockFlag = true;
//                while ((index = ringBuffer.tryNextReadIndex()) >= 0) {
//                    readEvent = ringBuffer.get(index);
//                    aioSession = readEvent.getSession();
//                    size = readEvent.getReadSize();
//                    ringBuffer.publishReadIndex(index);
//                    completed0(size, aioSession);
//                }
//                defendThreadBlockFlag = false;
//                synchronized (defendThreadLock) {
//                    defendThreadLock.notifyAll();
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