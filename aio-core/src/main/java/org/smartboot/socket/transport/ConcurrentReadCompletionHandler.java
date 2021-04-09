/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ConcurrentReadCompletionHandler.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
final class ConcurrentReadCompletionHandler extends ReadCompletionHandler {

    /**
     * 读回调资源信号量
     */
    private final Semaphore semaphore;

    private final ThreadLocal<ConcurrentReadCompletionHandler> threadLocal = new ThreadLocal<>();

    private final ThreadPoolExecutor threadPoolExecutor;

    ConcurrentReadCompletionHandler(final Semaphore semaphore, ThreadPoolExecutor threadPoolExecutor) {
        this.semaphore = semaphore;
        this.threadPoolExecutor = threadPoolExecutor;
    }


    @Override
    public void completed(final Integer result, final TcpAioSession aioSession) {
        if (threadLocal.get() != null) {
            super.completed(result, aioSession);
            return;
        }
        if (semaphore.tryAcquire()) {
            threadLocal.set(this);
            //处理当前读回调任务
            super.completed(result, aioSession);
            Runnable task;
            while ((task = threadPoolExecutor.getQueue().poll()) != null) {
                task.run();
            }
            semaphore.release();
            threadLocal.set(null);
            return;
        }
        //线程资源不足,暂时积压任务
        threadPoolExecutor.execute(() -> ConcurrentReadCompletionHandler.super.completed(result, aioSession));

    }
}