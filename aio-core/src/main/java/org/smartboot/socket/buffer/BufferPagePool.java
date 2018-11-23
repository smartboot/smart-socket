package org.smartboot.socket.buffer;

/**
 * ByteBuffer内存池
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public class BufferPagePool {

    private ThreadLocal<BufferPage> bufferPageThreadLocal;

    /**
     * @param pageSize 内存页大小
     * @param isDirect 是否使用直接缓冲区
     */
    public BufferPagePool(final int pageSize, final boolean isDirect) {
        bufferPageThreadLocal = new ThreadLocal<BufferPage>() {
            @Override
            protected BufferPage initialValue() {
                return new BufferPage(pageSize, isDirect);
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
    }
}
