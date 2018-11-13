package org.smartboot.socket.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ByteBuffer内存页
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public final class BufferPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPage.class);
    /**
     * 当前空闲的虚拟Buffer
     */
    private List<VirtualBuffer> freeList;
    /**
     * 当前内存页的归属池对象
     */
    private BufferPagePool bufferPool;
    private ByteBuffer buffer;

    /**
     * 待回收的缓存
     */
    private LinkedBlockingQueue<VirtualBuffer> unUsedList = new LinkedBlockingQueue<>();

    private AtomicInteger atomicInteger = new AtomicInteger();

    /**
     * @param bufferPool 当前
     * @param size
     * @param direct
     */
    BufferPage(BufferPagePool bufferPool, int size, boolean direct) {
        this.bufferPool = bufferPool;
        this.buffer = allocate0(size, direct);
        freeList = new LinkedList<>();
        freeList.add(new VirtualBuffer(this, buffer, buffer.position(), buffer.limit()));
//        new Thread() {
//            @Override
//            public void run() {
//                while (true) {
//
//                    try {
//                        System.out.println("Free " + atomicInteger.get() + " " + unUsedList.size() + " " + freeList);
//                        System.out.println("unUsed " + unUsedList);
//                        Thread.sleep(5000);
//                        System.gc();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();
    }

    /**
     * 申请物理内存页空间
     *
     * @param size
     * @param direct
     * @return
     */
    private ByteBuffer allocate0(int size, boolean direct) {
        return direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }

    public VirtualBuffer allocate(final int size) {
        try {
            if (freeList.isEmpty()) {
                clean();
            }
            if (freeList.isEmpty()) {
                LOGGER.warn("bufferPage has been used up");
                return new VirtualBuffer(null, allocate0(size, false), 0, 0);
            }


            int again = 2;
            while (again-- > 0) {
                Iterator<VirtualBuffer> iterator = freeList.iterator();
                VirtualBuffer bufferChunk = null;
                while (iterator.hasNext()) {
                    VirtualBuffer freeChunk = iterator.next();
//                check(freeChunk);
                    final int remaing = freeChunk.getParentLimit() - freeChunk.getParentPosition();
                    if (remaing < size) {
                        continue;
                    }
                    if (remaing == size) {
                        iterator.remove();
                        bufferChunk = freeChunk;
                    } else {
                        buffer.limit(freeChunk.getParentPosition() + size);
                        buffer.position(freeChunk.getParentPosition());
                        bufferChunk = new VirtualBuffer(this, buffer.slice(), buffer.position(), buffer.limit());
                        freeChunk.setParentPosition(buffer.limit());
                        buffer.limit(freeChunk.getParentLimit());
                        buffer.position(freeChunk.getParentPosition());
                        freeChunk.buffer(buffer.slice());
//                    check(freeChunk);
                    }
                    if (bufferChunk.buffer().remaining() != size) {
                        LOGGER.error(bufferChunk.buffer().remaining() + "aaaa" + size);
                        throw new RuntimeException("allocate " + size + ", buffer:" + bufferChunk);
                    }
//                check(bufferChunk);
//                check(freeChunk);
                    atomicInteger.incrementAndGet();
                    return bufferChunk;
                }
                clean();
            }
//            ByteBuf releaseBuf = null;
//            while ((releaseBuf = releaseList.poll()) != null) {
//                release0(releaseBuf);
//            }
            LOGGER.warn("bufferPage has no available space: " + size);
            return new VirtualBuffer(null, allocate0(size, false), 0, 0);
        } finally {
//            for (ByteBuf byteBuf : freeList) {
//                check(byteBuf);
//            }
        }
    }

    void addUnusedBuffer(VirtualBuffer virtualBuffer) {
        if (virtualBuffer == null) {
            return;
        }
        if (virtualBuffer.bufferPage != this) {
            throw new IllegalArgumentException();
        }
        atomicInteger.decrementAndGet();
        unUsedList.add(virtualBuffer);
//        LOGGER.info("回收成功:{}", virtualBuffer.hashCode() + "" + virtualBuffer);

    }

    public void clean() {
        VirtualBuffer buffer = null;
        while ((buffer = unUsedList.poll()) != null) {
            clean(buffer);
        }
    }

    private void clean(VirtualBuffer cleanBuffer) {
        try {
            cleanBuffer.buffer().clear();
            if (freeList.isEmpty()) {
                freeList.add(cleanBuffer);
//                check(releaseChunk);
                return;
            }
            int index = 0;
            Iterator<VirtualBuffer> iterator = freeList.iterator();
            while (iterator.hasNext()) {
                VirtualBuffer freeChunk = iterator.next();
//                check(freeChunk);
                //releaseChunk在freeChunk之前并且形成连续块
                if (freeChunk.getParentPosition() == cleanBuffer.getParentLimit()) {
                    freeChunk.setParentPosition(cleanBuffer.getParentPosition());
                    buffer.limit(freeChunk.getParentLimit());
                    buffer.position(freeChunk.getParentPosition());
                    freeChunk.buffer(buffer.slice());
//                    check(freeChunk);
                    return;
                }
                //releaseChunkfreeChunk之后并形成连续块
                if (freeChunk.getParentLimit() == cleanBuffer.getParentPosition()) {
                    freeChunk.setParentLimit(cleanBuffer.getParentLimit());
                    //判断后一个是否连续
                    if (iterator.hasNext()) {
                        VirtualBuffer next = iterator.next();
                        if (next.getParentPosition() == freeChunk.getParentLimit()) {
                            freeChunk.setParentLimit(next.getParentLimit());
                            iterator.remove();
                        } else if (next.getParentPosition() < freeChunk.getParentLimit()) {
                            throw new RuntimeException("");
                        }
                    }
                    buffer.limit(freeChunk.getParentLimit());
                    buffer.position(freeChunk.getParentPosition());
                    freeChunk.buffer(buffer.slice());
//                    check(freeChunk);

                    return;
                }
                if (freeChunk.getParentPosition() > cleanBuffer.getParentLimit()) {
                    cleanBuffer.buffer(cleanBuffer.buffer());
                    freeList.add(index, cleanBuffer);
//                    check(releaseChunk);
                    return;
                }
                index++;
            }
//            check(releaseChunk);
            cleanBuffer.buffer(cleanBuffer.buffer());
            freeList.add(cleanBuffer);
        } finally {
//            for (ByteBuf b : freeList) {
//                check(b);
//            }
        }
    }

    @Override
    public String toString() {
        return "BufferPage{" +
                "freeList=" + freeList +
                ", buffer=" + buffer +
                ", unUsedList=" + unUsedList +
                '}';
    }

    boolean hasFree() {
        return !freeList.isEmpty();
    }
}
