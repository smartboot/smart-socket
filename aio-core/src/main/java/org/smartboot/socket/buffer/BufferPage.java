package org.smartboot.socket.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public class BufferPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPage.class);
    public List<ByteBuf> freeList;
    private LinkedBlockingQueue<ByteBuf> releaseList = new LinkedBlockingQueue<>();
    private BufferPool bufferPool;
    private ByteBuffer buffer;

    BufferPage(BufferPool bufferPool, int size, boolean direct) {
        this.bufferPool = bufferPool;
        this.buffer = allocate(size, direct);
        freeList = new LinkedList<>();
        freeList.add(new ByteBuf(this, buffer, buffer.position(), buffer.limit()));
    }

    private ByteBuffer allocate(int size, boolean direct) {
        return direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }

    public synchronized ByteBuf allocate(final int size) {
        try {
//            if (freeList.isEmpty()) {
//                ByteBuf releaseBuf = null;
//                while ((releaseBuf = releaseList.poll()) != null) {
//                    release0(releaseBuf);
//                }
//            }
            if (freeList.isEmpty()) {
                LOGGER.warn("bufferPage has been used up");
                return new ByteBuf(null, allocate(size, false), 0, 0);
            }
            ByteBuf bufferChunk = null;
            Iterator<ByteBuf> iterator = freeList.iterator();
            while (iterator.hasNext()) {
                ByteBuf freeChunk = iterator.next();
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
                    bufferChunk = new ByteBuf(this, buffer.slice(), buffer.position(), buffer.limit());
                    freeChunk.setParentPosition(buffer.limit());
                    buffer.limit(freeChunk.getParentLimit());
                    buffer.position(freeChunk.getParentPosition());
                    freeChunk.buffer(buffer.slice());
//                    check(freeChunk);
                }
//            usedList.add(bufferChunk);
                if (bufferChunk.buffer().remaining() != size) {
                    LOGGER.error(bufferChunk.buffer().remaining() + "aaaa" + size);
                }
//                check(bufferChunk);
//                check(freeChunk);
                return bufferChunk;
            }
//            ByteBuf releaseBuf = null;
//            while ((releaseBuf = releaseList.poll()) != null) {
//                release0(releaseBuf);
//            }
            LOGGER.warn("bufferPage has no available space");
            return new ByteBuf(null, allocate(size, false), 0, 0);
        } finally {
//            for (ByteBuf byteBuf : freeList) {
//                check(byteBuf);
//            }
        }
    }

    void release0(ByteBuf releaseChunk) {
        if (releaseChunk != null && releaseChunk.bufferPage == this) {
            releaseList.add(releaseChunk);
        }
    }

    synchronized void release(ByteBuf releaseChunk) {
        try {
            if (releaseChunk == null || releaseChunk.bufferPage != this) {
                return;
            }
            releaseChunk.buffer().clear();
            if (freeList.isEmpty()) {
                freeList.add(releaseChunk);
//                check(releaseChunk);
                return;
            }
            int index = 0;
            Iterator<ByteBuf> iterator = freeList.iterator();
            while (iterator.hasNext()) {
                ByteBuf freeChunk = iterator.next();
//                check(freeChunk);
                //releaseChunk在freeChunk之前并且形成连续块
                if (freeChunk.getParentPosition() == releaseChunk.getParentLimit()) {
                    freeChunk.setParentPosition(releaseChunk.getParentPosition());
                    buffer.limit(freeChunk.getParentLimit());
                    buffer.position(freeChunk.getParentPosition());
                    freeChunk.buffer(buffer.slice());
//                    check(freeChunk);
                    return;
                }
                //releaseChunkfreeChunk之后并形成连续块
                if (freeChunk.getParentLimit() == releaseChunk.getParentPosition()) {
                    freeChunk.setParentLimit(releaseChunk.getParentLimit());
                    //判断后一个是否连续
                    if (iterator.hasNext()) {
                        ByteBuf next = iterator.next();
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
                if (freeChunk.getParentPosition() > releaseChunk.getParentLimit()) {
                    freeList.add(index, releaseChunk);
//                    check(releaseChunk);
                    return;
                }
                index++;
            }
//            check(releaseChunk);
            freeList.add(releaseChunk);
        } finally {
//            for (ByteBuf b : freeList) {
//                check(b);
//            }
        }
    }

//    void check(ByteBuf byteBuf) {
//        if (byteBuf.buffer().remaining() != (byteBuf.getParentLimit() - byteBuf.getParentPosition())) {
//            throw new RuntimeException(byteBuf.toString());
//        }
//    }

//    void clear() {
//        buffer.clear();
//        freeList.clear();
//        freeList.add(new ByteBuf(this, buffer, buffer.position(), buffer.limit()));
//    }

    BufferPool getBufferPool() {
        return bufferPool;
    }

    boolean hasFree() {
        return !freeList.isEmpty();
    }
}
