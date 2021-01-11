/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BufferPagePool.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.buffer;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ByteBuffer内存池
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public final class BufferPagePool {

    /**
     * 守护线程在空闲时期回收内存资源
     */
    private static final ScheduledThreadPoolExecutor BUFFER_POOL_CLEAN = new ScheduledThreadPoolExecutor(1, r -> {
        Thread thread = new Thread(r, "BufferPoolClean");
        thread.setDaemon(true);
        return thread;
    });
    /**
     * 内存页游标
     */
    private final AtomicInteger cursor = new AtomicInteger(0);
    /**
     * 内存页组
     */
    private BufferPage[] bufferPages;
    /**
     * 共享缓存页
     */
    private BufferPage sharedBufferPage;
    private boolean enabled = true;
    /**
     * 内存回收任务
     */
    private final ScheduledFuture<?> future = BUFFER_POOL_CLEAN.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
            if (enabled) {
                for (BufferPage bufferPage : bufferPages) {
                    bufferPage.tryClean();
                }
                if (sharedBufferPage != null) {
                    sharedBufferPage.tryClean();
                }
            } else {
                if (bufferPages != null) {
                    for (BufferPage page : bufferPages) {
                        page.release();
                    }
                    bufferPages = null;
                }
                if (sharedBufferPage != null) {
                    sharedBufferPage.release();
                    sharedBufferPage = null;
                }
                future.cancel(false);
            }
        }
    }, 500, 1000, TimeUnit.MILLISECONDS);

    /**
     * @param pageSize 内存页大小
     * @param pageNum  内存页个数
     * @param isDirect 是否使用直接缓冲区
     */
    public BufferPagePool(final int pageSize, final int pageNum, final boolean isDirect) {
        this(pageSize, pageNum, -1, isDirect);
    }

    /**
     * @param pageSize       内存页大小
     * @param pageNum        内存页个数
     * @param sharedPageSize 共享内存页大小
     * @param isDirect       是否使用直接缓冲区
     */
    public BufferPagePool(final int pageSize, final int pageNum, final int sharedPageSize, final boolean isDirect) {
        if (sharedPageSize > 0) {
            sharedBufferPage = new BufferPage(null, null, sharedPageSize, isDirect);
        }
        bufferPages = new BufferPage[pageNum];
        for (int i = 0; i < pageNum; i++) {
            bufferPages[i] = new BufferPage(bufferPages, sharedBufferPage, pageSize, isDirect);
        }
        if ((pageNum == 0 || pageSize == 0) && sharedPageSize <= 0) {
            future.cancel(false);
        }
    }

    /**
     * 申请FastBufferThread的线程对象,配合线程池申请会有更好的性能表现
     *
     * @param target Runnable
     * @param name   线程名
     * @return FastBufferThread线程对象
     */
    public Thread newThread(Runnable target, String name) {
        assertEnabled();
        return new FastBufferThread(target, name);
    }

    /**
     * 申请内存页
     *
     * @return 缓存页对象
     */
    public BufferPage allocateBufferPage() {
        assertEnabled();
        //轮训游标，均衡分配内存页
        return bufferPages[(cursor.getAndIncrement() & Integer.MAX_VALUE) % bufferPages.length];
    }

    private void assertEnabled() {
        if (!enabled) {
            throw new IllegalStateException("buffer pool is disable");
        }
    }

    /**
     * 释放回收内存
     */
    public void release() {
        enabled = false;
    }
}

