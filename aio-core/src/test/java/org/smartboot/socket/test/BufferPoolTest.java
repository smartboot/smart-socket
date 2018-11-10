package org.smartboot.socket.test;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.BufferPool;
import org.smartboot.socket.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/1
 */

public class BufferPoolTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPoolTest.class);

    public static void main(String[] args) {
        BufferPool bufferPool = new BufferPool(1024, 1024, true);
        BufferPage page = bufferPool.getBufferPage();
        int i = 0;
        while (i++ < 10) {
            ByteBuf chunk = page.allocate(10);
            ByteBuffer buffer = chunk.buffer();
            System.out.println(buffer + chunk.toString());

        }
    }

    @Test
    public void testPage() {
        BufferPool bufferPool = new BufferPool(10, 1024, true);
        final BufferPage page = bufferPool.getBufferPage();
        for (int j = 0; j < 10; j++) {
            new Thread() {
                @Override
                public void run() {
                    int i = 13000;
                    while (i-- > 0) {
                        int size = (int) (Math.random() * 5);
                        if (size == 0) {
                            continue;
                        }
                        ByteBuf byteBuf = page.allocate(size);
                        if (size != byteBuf.buffer().remaining()) {
                            System.out.println("errors");
                            throw new RuntimeException();
                        }
                        byteBuf.release();
//                    System.out.println(page.freeList);
                    }
                }
            }.start();
        }

    }

    @Test
    public void testA() {
        int num = 1000000;
        int i = 0;
        long start = System.currentTimeMillis();
        while (i++ < num) {
            ByteBuffer.allocate(1024);
        }
        System.out.println(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        i=0;
        while(i++<num){
            ByteBuffer.allocateDirect(1024);
        }
        System.out.println(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        i=0;
        BufferPool pool=new BufferPool(1024*4,1,true);
        BufferPage page=pool.getBufferPage();
        while (i++<num){
            ByteBuf byteBuf=page.allocate(1024);
            byteBuf.release();
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}
