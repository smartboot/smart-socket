package org.smartboot.socket.buffer;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ByteBuffer内存池
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public class BufferPagePool {
    /**
     * 守护线程在空闲时期回收内存资源
     */
    private static final ScheduledExecutorService BUFFER_POOL_CLEAN = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "BufferPoolClean");
            thread.setDaemon(true);
            return thread;
        }
    });
    /**
     * 内存页组
     */
    private BufferPage[] bufferPages;

    /**
     * 共享缓存页
     */
    private BufferPage sharedBufferPage;
    /**
     * 内存页游标
     */
    private AtomicInteger cursor = new AtomicInteger(0);

    /**
     * 线程游标
     */
    private AtomicInteger threadCursor = new AtomicInteger(0);

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
        if (pageNum <= 0) {
            throw new IllegalArgumentException("pageNum must greater than 0");
        }
        if (sharedPageSize > 0) {
            sharedBufferPage = new BufferPage(null, null, sharedPageSize, isDirect);
        }
        bufferPages = new BufferPage[pageNum];
        for (int i = 0; i < pageNum; i++) {
            bufferPages[i] = new BufferPage(bufferPages, sharedBufferPage, pageSize, isDirect);
        }

        BUFFER_POOL_CLEAN.scheduleWithFixedDelay(new TimerTask() {
            @Override
            public void run() {
                for (BufferPage bufferPage : bufferPages) {
                    bufferPage.tryClean();
                }
                if (sharedBufferPage != null) {
                    sharedBufferPage.tryClean();
                }
            }
        }, 500, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 申请FastBufferThread的线程对象,配合线程池申请会有更好的性能表现
     *
     * @param target Runnable
     * @param name   线程名
     * @return FastBufferThread线程对象
     */
    public Thread newThread(Runnable target, String name) {
        return new FastBufferThread(target, name, threadCursor.getAndIncrement() % bufferPages.length);
    }

    /**
     * 申请内存页
     *
     * @return 缓存页对象
     */
    public BufferPage allocateBufferPage() {
        //轮训游标，均衡分配内存页
        int index = cursor.getAndIncrement();
        if (index < 0) {
            cursor.set(0);
        }
        return bufferPages[index % bufferPages.length];
    }
}
