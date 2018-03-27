package org.smartboot.socket.pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/25
 */
public class DirectBufferPool extends ObjectPool<Integer, ByteBuffer> {

    private static final Logger LOGGER = LogManager.getLogger(DirectBufferPool.class);
    private static final int[] keyArray = new int[]{32, 64, 128, 256, 512, 1024, 2048, 4 * 1024,
            8 * 1024, 16 * 1024, 32 * 1024, 64 * 1024, 128 * 1024, 256 * 1024};
    private static DirectBufferPool pool = new DirectBufferPool();

    public static DirectBufferPool getPool() {
        return pool;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        int len = 1000000;
        for (int i = 0; i < len; i++) {
            ByteBuffer.allocateDirect(100);
        }
        System.out.println("DirectBuffer:" + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < len; i++) {
            ByteBuffer.allocate(100);
        }
        System.out.println("HeapBuffer:" + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < len; i++) {
            ByteBuffer b = DirectBufferPool.getPool().acquire(100);
            DirectBufferPool.getPool().release(b);
        }
        System.out.println("PoolBuffer:" + (System.currentTimeMillis() - start));
    }

    @Override
    public ByteBuffer acquire(Integer size) {
        if (true) {
            return ByteBuffer.allocateDirect(size);
        }
        if (size > keyArray[keyArray.length - 1]) {
            LOGGER.warn("acquire bytebuffer too big ,size is:{}", size);
//            return ByteBuffer.allocate(size);
            throw new UnsupportedOperationException("");
        }
        for (int key : keyArray) {
            if (key >= size) {
                ByteBuffer b = super.acquire(key);
                return b;
            }
        }
        throw new UnsupportedOperationException();
//        return ByteBuffer.allocate(size);
    }

    public void release(ByteBuffer t) {
        if (t == null || !t.isDirect()) {
            return;
        }
        for (int keySize : keyArray) {
            if (keySize == t.capacity()) {
                t.clear();
                super.release(t.capacity(), t);
                return;
            }
        }
    }

    @Override
    public ByteBuffer init(Integer key) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("init byteBuffer, size:{}", key);
        }
        return ByteBuffer.allocateDirect(key);
    }
}
