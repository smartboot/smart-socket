package org.smartboot.socket.buffer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ByteBuffer内存池
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public class BufferPagePool {
    private static Timer timer = new Timer("BufferPoolClean", true);
    private BufferPage[] bufferPageList;
    /**
     * 内存页游标
     */
    private AtomicInteger cursor = new AtomicInteger(0);

    /**
     * @param pageSize 内存页大小
     * @param poolSize 内存页个数
     * @param isDirect 是否使用直接缓冲区
     */
    public BufferPagePool(final int pageSize, final int poolSize, final boolean isDirect) {
        bufferPageList = new BufferPage[poolSize];
        for (int i = 0; i < poolSize; i++) {
            bufferPageList[i] = new BufferPage(pageSize, isDirect);
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (BufferPage bufferPage : bufferPageList) {
                    bufferPage.tryClean();
                }
            }
        }, 500, 1000);
    }

    /**
     * 申请内存页
     *
     * @return 缓存页对象
     */
    public BufferPage allocateBufferPage() {
        //轮训游标，均衡分配内存页
        return bufferPageList[cursor.getAndIncrement() % bufferPageList.length];
    }
}
