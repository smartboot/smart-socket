/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ConcurrentReadCompletionHandler.java
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
final class ConcurrentReadCompletionHandler<T> extends ReadCompletionHandler<T> {

    /**
     * 读回调资源信号量
     */
    private Semaphore semaphore;

    private ThreadLocal<ConcurrentReadCompletionHandler> threadLocal = new ThreadLocal<>();

    private ExecutorService executorService = new ThreadPoolExecutor(0, 1,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    ConcurrentReadCompletionHandler(final Semaphore semaphore) {
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
            semaphore.release();
            threadLocal.set(null);
            return;
        }
        //线程资源不足,暂时积压任务
        executorService.execute(() -> {
            ConcurrentReadCompletionHandler.super.completed(result, aioSession);
        });

    }

    /**
     * 停止内部线程
     */
    public void shutdown() {
        executorService.shutdown();
    }
}