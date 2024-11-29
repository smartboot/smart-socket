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
    public static final BufferPagePool DEFAULT_BUFFER_PAGE_POOL = new BufferPagePool(0, 1, false);
    /**
     * 内存页游标
     */
    private final AtomicInteger cursor = new AtomicInteger(0);
    /**
     * 内存页组
     */
    private AbstractBufferPage[] bufferPages;
    private boolean enabled = true;
    /**
     * 内存回收任务
     */
    private final ScheduledFuture<?> future;

    public BufferPagePool(int pageNum, boolean direct) {
        this(0, pageNum, direct);
    }

    /**
     * @param pageSize 内存页大小
     * @param pageNum  内存页个数
     * @param isDirect 是否使用直接缓冲区
     */
    public BufferPagePool(final int pageSize, final int pageNum, final boolean isDirect) {
        bufferPages = new AbstractBufferPage[pageNum];
        for (int i = 0; i < pageNum; i++) {
            bufferPages[i] = pageSize == 0 ? new ElasticBufferPage(isDirect) : new StaticBufferPage(pageSize, isDirect);
        }
        if (pageNum > 0) {
            future = BUFFER_POOL_CLEAN.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if (enabled) {
                        for (AbstractBufferPage bufferPage : bufferPages) {
                            bufferPage.tryClean();
                        }
                    } else {
                        if (bufferPages != null) {
                            for (AbstractBufferPage page : bufferPages) {
                                page.release();
                            }
                            bufferPages = null;
                        }
                        future.cancel(false);
                    }
                }
            }, 500, 1000, TimeUnit.MILLISECONDS);
        } else {
            future = null;
        }
    }


    /**
     * 申请内存页
     *
     * @return 缓存页对象
     */
    public BufferPage allocateBufferPage() {
        if (enabled) {
            //轮训游标，均衡分配内存页
            return bufferPages[(cursor.getAndIncrement() & Integer.MAX_VALUE) % bufferPages.length];
        }
        throw new IllegalStateException("buffer pool is disable");
    }


    /**
     * 释放回收内存
     */
    public void release() {
        enabled = false;
    }


}

