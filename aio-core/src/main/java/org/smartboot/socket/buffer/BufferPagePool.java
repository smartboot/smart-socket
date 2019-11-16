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
    private BufferPage[] bufferPageList;
    /**
     * 内存页游标
     */
    private AtomicInteger cursor = new AtomicInteger(0);

    private AtomicInteger threadCursor = new AtomicInteger(0);

    /**
     * @param pageSize 内存页大小
     * @param poolSize 内存页个数
     * @param isDirect 是否使用直接缓冲区
     */
    public BufferPagePool(final int pageSize, final int poolSize, final boolean isDirect) {
        bufferPageList = new BufferPage[poolSize];
        for (int i = 0; i < poolSize; i++) {
            bufferPageList[i] = new BufferPage(bufferPageList, pageSize, isDirect);
        }
        BUFFER_POOL_CLEAN.scheduleWithFixedDelay(new TimerTask() {
            @Override
            public void run() {
                for (BufferPage bufferPage : bufferPageList) {
                    bufferPage.tryClean();
                }
            }
        }, 500, 1000, TimeUnit.MILLISECONDS);
    }

    public Thread newThread(Runnable target, String name) {
        return new FastBufferThread(target, name, threadCursor.getAndIncrement() % bufferPageList.length);
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
        return bufferPageList[index % bufferPageList.length];
    }
}
