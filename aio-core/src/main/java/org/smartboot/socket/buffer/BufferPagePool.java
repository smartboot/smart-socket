package org.smartboot.socket.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ByteBuffer内存池
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public class BufferPagePool {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPagePool.class);
//    private BufferPage[] bufferPageList;

    private ThreadLocal<BufferPage> bufferPageThreadLocal;

    /**
     * 内存页游标
     */
    private int cursor = -1;

    /**
     * 内存页大小,等同于ByteBuffer.allocateDirect(size) 或 ByteBuffer.allocate(size)
     */
    private int pageSize;

    /**
     * @param pageSize 内存页大小
     * @param poolSize 内存页个数
     * @param isDirect 是否使用直接缓冲区
     */
    public BufferPagePool(final int pageSize, final int poolSize, final boolean isDirect) {
        this.pageSize = pageSize;
//        bufferPageList = new BufferPage[poolSize];
//        for (int i = 0; i < poolSize; i++) {
//            bufferPageList[i] = new BufferPage(this, pageSize, isDirect);
//        }
        bufferPageThreadLocal = new ThreadLocal<BufferPage>() {
            @Override
            protected BufferPage initialValue() {
                return new BufferPage(BufferPagePool.this, pageSize, isDirect);
            }
        };
    }

    /**
     * 申请内存页
     *
     * @return
     */
    public BufferPage allocateBufferPage() {
        return bufferPageThreadLocal.get();
//        //轮训游标，均衡分配内存页
//        for (int i = 1; i < bufferPageList.length; i++) {
//            cursor = (i + cursor) % bufferPageList.length;
//            BufferPage page = bufferPageList[cursor];
//            if (page.hasFree()) {
//                return page;
//            }
//        }
//        LOGGER.debug("create temp bufferPage");
//        //生成临时Page,内存管理交由JVM
//        return new BufferPage(this, pageSize, false);
    }
}
