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
    public static final BufferPagePool DEFAULT_BUFFER_PAGE_POOL = new BufferPagePool(1, false);
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
    private static boolean directSupported = true;

    static {
        String version = System.getProperty("java.version");
        // 处理类似"1.8.0_301"和"9+"两种版本格式
        int majorVersion = version.startsWith("1.") ? Integer.parseInt(version.split("\\.")[1]) : Integer.parseInt(version.split("\\.")[0]);

        if (majorVersion > 8) {
            directSupported = false;
        }
    }

    /**
     * @param pageNum  内存页个数
     * @param isDirect 是否使用直接缓冲区
     */
    public BufferPagePool(final int pageNum, final boolean isDirect) {
        if (isDirect && !directSupported) {
            throw new IllegalStateException("当前版本的 smart-socket 申请 Direct ByteBuffer 要求 JDK 版本必须 <= 1.8，或者升级 smart-socket 至 1.6.x 版本");
        }
        bufferPages = new AbstractBufferPage[pageNum];
        for (int i = 0; i < pageNum; i++) {
            bufferPages[i] = new ElasticBufferPage(isDirect);
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
     * 按顺序从内存页组中分配指定大小的虚拟缓冲区。
     * 该方法会按顺序依次从内存页中获取缓冲区，使用原子计数器确保顺序分配。
     *
     * @param size 要分配的缓冲区大小
     * @return 分配的虚拟缓冲区
     */
    public VirtualBuffer allocateSequentially(final int size) {
        return bufferPages[(cursor.getAndIncrement() & Integer.MAX_VALUE) % bufferPages.length].allocate(size);
    }

    /**
     * 根据当前线程 ID 从内存页组中分配指定大小的虚拟缓冲区。
     * 该方法会根据当前线程的 ID 对内存页数量取模，将缓冲区分配到对应的内存页。
     *
     * @param size 要分配的缓冲区大小
     * @return 分配的虚拟缓冲区
     */
    public VirtualBuffer allocateByThreadId(final int size) {
        return bufferPages[(int) ((Thread.currentThread().getId()) % bufferPages.length)].allocate(size);
    }

    /**
     * 释放回收内存
     */
    public void release() {
        enabled = false;
    }

    @Override
    public String toString() {
        String logger = "";
        for (BufferPage page : bufferPages) {
            logger += "\r\n" + page.toString();
        }
        return logger;
    }
}

