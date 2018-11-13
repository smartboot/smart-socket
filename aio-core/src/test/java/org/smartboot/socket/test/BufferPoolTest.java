package org.smartboot.socket.test;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/1
 */

public class BufferPoolTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPoolTest.class);

    public static void main(String[] args) {
        BufferPagePool bufferPool = new BufferPagePool(1024, 1024, true);
        BufferPage page = bufferPool.allocateBufferPage();
        int i = 0;
        while (i++ < 10) {
            VirtualBuffer chunk = page.allocate(10);
            ByteBuffer buffer = chunk.buffer();
            System.out.println(buffer + chunk.toString());

        }
    }

    @Test
    public void testPage() {
        BufferPagePool bufferPool = new BufferPagePool(10, 1024, true);
        final BufferPage page = bufferPool.allocateBufferPage();
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
                        VirtualBuffer byteBuf = page.allocate(size);
                        if (size != byteBuf.buffer().remaining()) {
                            System.out.println("errors");
                            throw new RuntimeException();
                        }
                        byteBuf.clean();
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
        i = 0;
        while (i++ < num) {
            ByteBuffer.allocateDirect(1024);
        }
        System.out.println(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        i = 0;
        BufferPagePool pool = new BufferPagePool(1024 * 4, 1, true);
        BufferPage page = pool.allocateBufferPage();
        while (i++ < num) {
            VirtualBuffer byteBuf = page.allocate(1024);
            byteBuf.clean();
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testB() {
        BufferPagePool pagePool = new BufferPagePool(1024, 1, true);
        int i = 0;
//        Demo1.a(pagePool);
//        pagePool.allocateBufferPage().allocate(4).clean();
//        pagePool.allocateBufferPage().allocate(4).clean();
//        pagePool.allocateBufferPage().allocate(4).clean();
//        pagePool.allocateBufferPage().allocate(4).clean();
//        pagePool.allocateBufferPage().allocate(3).clean();
//        pagePool.allocateBufferPage().allocate(4).clean();
//        pagePool.allocateBufferPage().allocate(4).clean();
//        pagePool.allocateBufferPage().allocate(5).clean();
//        pagePool.allocateBufferPage().allocate(3).clean();
//        pagePool.allocateBufferPage().allocate(3).clean();
//        pagePool.allocateBufferPage().allocate(4).clean();
//        pagePool.allocateBufferPage().allocate(3).clean();
//        pagePool.allocateBufferPage().allocate(3).clean();
//        pagePool.allocateBufferPage().allocate(3).clean();
//        pagePool.allocateBufferPage().allocate(5).clean();
        while (i++ < 100000) {
            int size = 100 + (int) (Math.random() * 100);
//            System.out.println(size + " " + i);
//            System.out.println("pagePool.allocateBufferPage().allocate(" + size + ").clean();");

            pagePool.allocateBufferPage().allocate(size).clean();
        }
        pagePool.allocateBufferPage().clean();
        System.out.println(pagePool.allocateBufferPage());
    }

}
