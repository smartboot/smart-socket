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
import org.smartboot.socket.buffer.RingBuffer;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
class TcpReadCompletionHandler<T> implements CompletionHandler<Integer, TcpAioSession<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpReadCompletionHandler.class);
    private static final int SPIN_LOCK_TIMES = Short.MAX_VALUE;
    /**
     * 读回调资源信号量
     */
    private Semaphore semaphore;
    /**
     * 递归线程标识
     */
    private ThreadLocal<CompletionHandler> recursionThreadLocal = null;
    private RingBuffer<TcpReadEvent> ringBuffer;

    private ConcurrentLinkedQueue<TcpAioSession<T>> aioSessions;

    private volatile boolean needNotify = true;

    private ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public TcpReadCompletionHandler() {
    }

    public TcpReadCompletionHandler(final RingBuffer<TcpReadEvent> ringBuffer, final ThreadLocal<CompletionHandler> recursionThreadLocal, Semaphore semaphore) {
        this.semaphore = semaphore;
        this.recursionThreadLocal = recursionThreadLocal;
        this.ringBuffer = ringBuffer;
//        Thread t = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        int consumerIndex = ringBuffer.nextReadIndex();
//                        TcpReadEvent readEvent = ringBuffer.get(consumerIndex);
//                        TcpAioSession aioSession = readEvent.getSession();
//                        int size = readEvent.getReadSize();
//                        ringBuffer.publishReadIndex(consumerIndex);
//                        completed0(size, aioSession);
//                        synchronized (this) {
//                            this.wait(100);
//                        }
//                    } catch (InterruptedException e) {
//                        LOGGER.error("", e);
//                    }
//                }
//            }
//        }, "smart-socket:DaemonThread");
//        t.setDaemon(true);
//        t.setPriority(1);
//        t.start();
        this.aioSessions = new ConcurrentLinkedQueue<>();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TcpAioSession aioSession = aioSessions.poll();
                        if (aioSession != null) {
                            completed0(aioSession.getLastReadSize(), aioSession);
                            synchronized (this) {
                                this.wait(100);
                            }
                            continue;
                        }
                        if (!lock.tryLock()) {
                            synchronized (this) {
                                this.wait(100);
                            }
                            continue;
                        }
                        try {
//                            LOGGER.info("wait");
                            needNotify = true;
                            notEmpty.await();
                        } finally {
                            lock.unlock();
                        }

                    } catch (InterruptedException e) {
                        LOGGER.error("", e);
                    }
                }
            }
        }, "smart-socket:DaemonThread");
        t.setDaemon(true);
        t.setPriority(1);
        t.start();
    }


    @Override
    public void completed(final Integer result, final TcpAioSession<T> aioSession) {
        aioSession.setLastReadSize(result);
        if (recursionThreadLocal == null || recursionThreadLocal.get() != null) {
            completed0(result, aioSession);
            return;
        }
        int spinLock = SPIN_LOCK_TIMES;
        do {
            if (semaphore.tryAcquire()) {
                if (spinLock < SPIN_LOCK_TIMES) {
                    LOGGER.info("spin lock success");
                }
                try {
                    recursionThreadLocal.set(this);
                    completed0(result, aioSession);
                    runRingBufferTask();
                } finally {
                    recursionThreadLocal.remove();
                    semaphore.release();
                }
                return;
            }
        } while ((spinLock >>= 1) > 0);
        aioSessions.offer(aioSession);
        if (needNotify && lock.tryLock()) {
            try {
                needNotify = false;
                notEmpty.signalAll();
            } finally {
                lock.unlock();
            }
        }
//        try {
//            int sequence = ringBuffer.nextWriteIndex();
//            TcpReadEvent readEvent = ringBuffer.get(sequence);
//            readEvent.setSession(aioSession);
//            readEvent.setReadSize(result);
//            ringBuffer.publishWriteIndex(sequence);
//        } catch (InterruptedException e) {
//            LOGGER.error("InterruptedException", e);
//        }
    }

    /**
     * 执行异步队列中的任务
     */
    void runRingBufferTask() {
        if (aioSessions == null) {
            return;
        }
        TcpAioSession<T> aioSession;
        while ((aioSession = aioSessions.poll()) != null) {
            completed0(aioSession.getLastReadSize(), aioSession);
        }
//        if (ringBuffer == null) {
//            return;
//        }
//        int index;
//        TcpReadEvent readEvent;
//        TcpAioSession<T> aioSession;
//        int size;
//        while ((index = ringBuffer.tryNextReadIndex()) >= 0) {
//            readEvent = ringBuffer.get(index);
//            aioSession = readEvent.getSession();
//            size = readEvent.getReadSize();
//            ringBuffer.publishReadIndex(index);
//            completed0(size, aioSession);
//        }
    }

    private void completed0(final Integer result, final TcpAioSession<T> aioSession) {
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
    public void failed(Throwable exc, TcpAioSession<T> aioSession) {
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