/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WriteCompletionHandler.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
class ConcurrentWriteCompletionHandler<T> extends WriteCompletionHandler<T> {

    /**
     * 读回调资源信号量
     */
    private Semaphore semaphore;

    private ThreadLocal<ConcurrentWriteCompletionHandler> threadLocal = new ThreadLocal<>();

    private LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private ExecutorService executorService = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
            60L, TimeUnit.SECONDS, taskQueue);

    public ConcurrentWriteCompletionHandler(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    @Override
    public void completed(final Integer result, final TcpAioSession<T> aioSession) {
        if (threadLocal.get() != null) {
            super.completed(result, aioSession);
            return;
        }
        if (semaphore.tryAcquire()) {
            threadLocal.set(this);
            //处理当前读回调任务
            super.completed(result, aioSession);
            Runnable task;
            while ((task = taskQueue.poll()) != null) {
                task.run();
            }
            semaphore.release();
            threadLocal.set(null);
            return;
        }
        //线程资源不足,暂时积压任务
        executorService.execute(() -> {
            System.out.println("executor");
            ConcurrentWriteCompletionHandler.super.completed(result, aioSession);
        });
    }

}