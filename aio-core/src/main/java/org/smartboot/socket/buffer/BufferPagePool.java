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
 * ByteBuffer内存池管理类，负责创建和管理多个BufferPage实例。
 * 该类提供了内存分配、回收和释放的功能，通过内存池化技术减少频繁创建和销毁ByteBuffer带来的性能开销。
 * 支持堆内存和堆外内存（直接缓冲区）两种模式，并提供不同的分配策略。
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public final class BufferPagePool {
    /**
     * 守护线程池，用于在空闲时期定期回收内存资源。
     * 使用单线程的ScheduledThreadPoolExecutor，并将线程设置为守护线程，
     * 这样在JVM退出时，线程会自动终止，不会阻止程序的正常退出。
     */
    private static final ScheduledThreadPoolExecutor BUFFER_POOL_CLEAN = new ScheduledThreadPoolExecutor(1, r -> {
        Thread thread = new Thread(r, "BufferPoolClean");
        thread.setDaemon(true);
        return thread;
    });
    /**
     * 默认的BufferPagePool实例，使用1个堆内存页。
     * 提供给不需要自定义内存池配置的场景使用，简化API使用。
     */
    public static final BufferPagePool DEFAULT_BUFFER_PAGE_POOL = new BufferPagePool(1, false);
    /**
     * 内存页分配游标，用于顺序分配策略。
     * 使用原子整数确保在多线程环境下的线程安全，避免多个线程获取到同一个内存页索引。
     */
    private final AtomicInteger cursor = new AtomicInteger(0);
    /**
     * 内存页数组，存储所有由该内存池管理的BufferPage实例。
     * 数组长度由构造函数中的pageNum参数决定。
     */
    private BufferPage[] bufferPages;
    /**
     * 内存池启用状态标志。
     * 当值为true时，表示内存池处于启用状态，可以分配内存和定期回收；
     * 当值为false时，表示内存池已被禁用，不再分配新内存，并释放已分配的内存。
     */
    private boolean enabled = true;
    /**
     * 内存回收定时任务的Future对象。
     * 用于控制定时任务的执行和取消，在内存池释放时可以通过该对象取消定时任务。
     */
    private final ScheduledFuture<?> future;


    /**
     * 构造一个内存池对象。
     *
     * @param pageNum  内存页个数，决定了内存池中BufferPage实例的数量
     * @param isDirect 是否使用直接缓冲区。当值为true时，使用堆外内存；当值为false时，使用堆内存
     * @throws IllegalStateException 当在不支持直接缓冲区的JDK版本中尝试使用直接缓冲区时抛出异常
     */
    public BufferPagePool(final int pageNum, boolean isDirect) {
        // 检查JDK版本对直接缓冲区的支持情况
        if (isDirect && !DirectBufferCleaner.isDirectSupported()) {
            isDirect = false;
            System.err.println("The current version of JDK does not support applying for Direct ByteBuffer.");
            System.err.println("Automatically downgraded to Heap ByteBuffer...");
        }
        // 创建指定数量的内存页
        bufferPages = new BufferPage[pageNum];
        for (int i = 0; i < pageNum; i++) {
            bufferPages[i] = new BufferPage(isDirect);
        }
        // 如果内存页数量大于0，则启动定时回收任务
        if (pageNum > 0) {
            future = BUFFER_POOL_CLEAN.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if (enabled) {
                        // 内存池启用状态下，尝试回收每个内存页中的缓冲区
                        for (BufferPage bufferPage : bufferPages) {
                            bufferPage.tryClean();
                        }
                    } else {
                        // 内存池禁用状态下，释放所有内存页资源并取消定时任务
                        if (bufferPages != null) {
                            for (BufferPage page : bufferPages) {
                                page.release();
                            }
                            bufferPages = null;
                        }
                        future.cancel(false);
                    }
                }
            }, 1, 5, TimeUnit.SECONDS); // 初始延迟1s，之后每5s执行一次
        } else {
            future = null;
        }
    }

    /**
     * 从内存池中分配一个BufferPage实例。
     * 该方法使用简单的轮询（Round Robin）策略来分配内存页，通过原子操作保证多线程环境下的线程安全。
     * 使用无符号右移掩码（& Integer.MAX_VALUE）确保索引值为正数，避免负数索引问题。
     *
     * @return 分配的BufferPage实例
     */
    public BufferPage allocatePage() {
        return bufferPages[(cursor.getAndIncrement() & Integer.MAX_VALUE) % bufferPages.length];
    }

    /**
     * 释放内存池中的所有资源。
     * 该方法不会立即释放资源，而是通过将enabled标志设置为false，
     * 让定时任务在下一次执行时检测到状态变化并执行实际的资源释放操作。
     * 这种设计可以避免在调用线程中执行耗时的资源释放操作。
     */
    public void release() {
        enabled = false;
    }

    /**
     * 返回内存池的字符串表示形式，用于调试和日志记录。
     * 该方法会遍历所有内存页，并将它们的字符串表示形式拼接起来。
     *
     * @return 包含所有内存页信息的字符串
     */
    @Override
    public String toString() {
        String logger = "";
        for (BufferPage page : bufferPages) {
            logger += "\r\n" + page.toString();
        }
        return logger;
    }
}

