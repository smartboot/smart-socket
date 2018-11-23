package org.smartboot.socket.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

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
    private ByteBuffer buffer;

    /**
     * 待回收的缓存
     */
    private LinkedBlockingQueue<VirtualBuffer> unUsedList = new LinkedBlockingQueue<>();

    /**
     * @param size
     * @param direct
     */
    BufferPage(int size, boolean direct) {
        freeList = new LinkedList<>();
        if (size > 0) {
            this.buffer = allocate0(size, direct);
            freeList.add(new VirtualBuffer(this, buffer, buffer.position(), buffer.limit()));
        }
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
        if (freeList.isEmpty()) {
            clean();
        }
        if (freeList.isEmpty()) {
//            LOGGER.warn("bufferPage has been used up");
            return new VirtualBuffer(null, allocate0(size, false), 0, 0);
        }


        int again = 2;
        while (again-- > 0) {
            Iterator<VirtualBuffer> iterator = freeList.iterator();
            VirtualBuffer bufferChunk = null;
            while (iterator.hasNext()) {
                VirtualBuffer freeChunk = iterator.next();
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
                }
                if (bufferChunk.buffer().remaining() != size) {
                    LOGGER.error(bufferChunk.buffer().remaining() + "aaaa" + size);
                    throw new RuntimeException("allocate " + size + ", buffer:" + bufferChunk);
                }
                return bufferChunk;
            }
            clean();
        }
        LOGGER.warn("bufferPage has no available space: " + size);
        return new VirtualBuffer(null, allocate0(size, false), 0, 0);
    }

    void addUnusedBuffer(VirtualBuffer virtualBuffer) {
        if (virtualBuffer == null) {
            return;
        }
        if (virtualBuffer.bufferPage != this) {
            throw new IllegalArgumentException();
        }
        unUsedList.add(virtualBuffer);
    }

    public void clean() {
        VirtualBuffer buffer = null;
        while ((buffer = unUsedList.poll()) != null) {
            clean(buffer);
        }
    }

    private void clean(VirtualBuffer cleanBuffer) {
        cleanBuffer.buffer().clear();
        if (freeList.isEmpty()) {
            freeList.add(cleanBuffer);
            return;
        }
        int index = 0;
        Iterator<VirtualBuffer> iterator = freeList.iterator();
        while (iterator.hasNext()) {
            VirtualBuffer freeChunk = iterator.next();
            //releaseChunk在freeChunk之前并且形成连续块
            if (freeChunk.getParentPosition() == cleanBuffer.getParentLimit()) {
                freeChunk.setParentPosition(cleanBuffer.getParentPosition());
                buffer.limit(freeChunk.getParentLimit());
                buffer.position(freeChunk.getParentPosition());
                freeChunk.buffer(buffer.slice());
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
                return;
            }
            if (freeChunk.getParentPosition() > cleanBuffer.getParentLimit()) {
                cleanBuffer.buffer(cleanBuffer.buffer());
                freeList.add(index, cleanBuffer);
                return;
            }
            index++;
        }
        cleanBuffer.buffer(cleanBuffer.buffer());
        freeList.add(cleanBuffer);
    }
}
