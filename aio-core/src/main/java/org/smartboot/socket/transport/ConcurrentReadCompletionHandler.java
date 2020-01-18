/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ConcurrentReadCompletionHandler.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.smartboot.socket.NetMonitor;

import java.util.concurrent.ConcurrentLinkedDeque;
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
final class ConcurrentReadCompletionHandler<T> extends ReadCompletionHandler<T> implements Runnable {

    /**
     * 读回调资源信号量
     */
    private Semaphore semaphore;
    /**
     * 读会话缓存队列
     */
    private ConcurrentLinkedQueue<TcpAioSession<T>> cacheAioSessionQueue = new ConcurrentLinkedQueue<>();

    private ConcurrentLinkedDeque<TcpAioSession<T>> willReadAioSessionQueue = new ConcurrentLinkedDeque<>();
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
    private boolean running = true;

    private ThreadLocal<ConcurrentReadCompletionHandler> threadLocal = new ThreadLocal<>();

    ConcurrentReadCompletionHandler(final Semaphore semaphore) {
        this.semaphore = semaphore;
        Thread watcherThread = new Thread(this, "smart-socket:watcher");
        watcherThread.setDaemon(true);
        watcherThread.setPriority(1);
        watcherThread.start();
    }


    @Override
    public void completed(final Integer result, final TcpAioSession<T> aioSession) {
        if (threadLocal.get() == null) {
            threadLocal.set(this);
        } else {
            super.completed(result, aioSession);
            return;
        }
        if (semaphore.tryAcquire()) {
            //处理当前读回调任务
            super.completed(result, aioSession);
            //执行缓存中的读回调任务
            TcpAioSession<T> cacheSession = null;
            while ((cacheSession = cacheAioSessionQueue.poll()) != null) {
                this.completed0(cacheSession);
                willReadAioSessionQueue.offer(cacheSession);
            }
            semaphore.release();
            threadLocal.set(null);
            //注册下一轮读事件
            while ((cacheSession = willReadAioSessionQueue.poll()) != null) {
                try {
                    cacheSession.continueRead();
                } catch (Exception e) {
                    failed(e, cacheSession);
                }
            }
            return;
        }
        threadLocal.set(null);
        aioSession.setLastReadSize(result);
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
        Thread.yield();
    }


    private void completed0(TcpAioSession<T> session) {
        try {
            // 接收到的消息进行预处理
            NetMonitor<T> monitor = session.getServerConfig().getMonitor();
            if (monitor != null) {
                monitor.afterRead(session, session.getLastReadSize());
            }
            //触发读回调
            session.readCompleted(session.getLastReadSize() == -1);
        } catch (Exception e) {
            failed(e, session);
        }
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