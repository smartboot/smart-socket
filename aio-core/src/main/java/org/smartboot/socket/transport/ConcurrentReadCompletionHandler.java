/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ReadCompletionHandler.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
final class ConcurrentReadCompletionHandler<T> extends ReadCompletionHandler<T> implements Runnable {

    /**
     * 读回调资源信号量
     */
    private Semaphore semaphore;
    /**
     * 读会话缓存队列
     */
    private ConcurrentLinkedQueue<TcpAioSession<T>> cacheAioSessionQueue = new ConcurrentLinkedQueue<>();
    /**
     * 应该可以不用volatile
     */
    private boolean needNotify = true;
    /**
     * 同步锁
     */
    private ReentrantLock lock = new ReentrantLock();
    /**
     * 非空条件
     */
    private final Condition notEmpty = lock.newCondition();
    private AtomicLong cursor = new AtomicLong();
    private boolean running = true;

    ConcurrentReadCompletionHandler(final Semaphore semaphore) {
        this.semaphore = semaphore;
        Thread watcherThread = new Thread(this, "smart-socket:watcher");
        watcherThread.setDaemon(true);
        watcherThread.setPriority(1);
        watcherThread.start();
    }


    @Override
    public void completed(final Integer result, final TcpAioSession<T> aioSession) {
        if (aioSession.getThreadReference().get() == Thread.currentThread()) {
            TcpAioSession<T> nextSession = cacheAioSessionQueue.poll();
            if (nextSession != null) {
                this.completed0(nextSession);
            }
            super.completed(result, aioSession);
            return;
        }
        aioSession.setLastReadSize(result);
        if (semaphore.tryAcquire()) {
            this.completed0(aioSession);
            runRingBufferTask();
            semaphore.release();
            return;
        }
        //线程资源不足,暂时积压任务
        cacheAioSessionQueue.offer(aioSession);
        if (needNotify && lock.tryLock()) {
            try {
                needNotify = false;
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 执行异步队列中的任务
     */
    private void runRingBufferTask() {
        TcpAioSession<T> session = null;
        long curStep = cursor.incrementAndGet();
        int tryCount = 8;
        while (--tryCount > 0 || curStep >= cursor.get()) {
            if ((session = cacheAioSessionQueue.poll()) == null) {
                break;
            }
            this.completed0(session);
        }
        cursor.decrementAndGet();
    }

    private void completed0(TcpAioSession<T> session) {
        session.getThreadReference().set(Thread.currentThread());
        super.completed(session.getLastReadSize(), session);
        session.getThreadReference().compareAndSet(Thread.currentThread(), null);
    }

    /**
     * 停止内部线程
     */
    public void shutdown() {
        running = false;
        lock.lock();
        try {
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * watcher线程,当存在待处理的读回调事件时，或许可以激活空闲状态的IO线程组
     */
    @Override
    public void run() {
        while (running) {
            try {
                TcpAioSession<T> aioSession = cacheAioSessionQueue.poll();
                if (aioSession != null) {
                    super.completed(aioSession.getLastReadSize(), aioSession);
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
                    needNotify = true;
                    notEmpty.await();
                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}