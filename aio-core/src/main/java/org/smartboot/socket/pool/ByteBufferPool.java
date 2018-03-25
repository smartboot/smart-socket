package org.smartboot.socket.pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/25
 */
public class ByteBufferPool extends ObjectPool<Integer, ByteBuffer> {

    private static final Logger LOGGER = LogManager.getLogger(ByteBufferPool.class);
    private static final int[] keyArray = new int[]{32, 64, 128, 256, 512, 1024, 2048, 4096, 256 * 1024};
    private static ByteBufferPool pool = new ByteBufferPool();

    public static ByteBufferPool getPool() {
        return pool;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        int len = 1000000;
        for (int i = 0; i < len; i++) {
            ByteBuffer.allocateDirect(100);
        }
        System.out.println(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        for (int i = 0; i < len; i++) {
            ByteBuffer b = ByteBufferPool.getPool().acquire(100);
            ByteBufferPool.getPool().release(b);
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    @Override
    public ByteBuffer acquire(Integer size) {
        if (size > keyArray[keyArray.length - 1]) {
            LOGGER.warn("acquire bytebuffer too big ,size is:{}", size);
//            return ByteBuffer.allocate(size);
            throw new UnsupportedOperationException();
        }
        for (int key : keyArray) {
            if (key >= size) {
                ByteBuffer b = super.acquire(key);
                b.clear();
                return b;
            }
        }
        throw new UnsupportedOperationException();
//        return ByteBuffer.allocate(size);
    }

    public void release(ByteBuffer t) {
        if (t.isDirect()) {
            super.release(t.capacity(), t);
        }
    }

    @Override
    public ByteBuffer init(Integer key) {
        LOGGER.info("init byteBuffer, size:{}", key);
        return ByteBuffer.allocateDirect(key);
    }
}
