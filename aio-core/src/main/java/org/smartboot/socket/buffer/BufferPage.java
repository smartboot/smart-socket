package org.smartboot.socket.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
     * @param size
     * @param direct
     */
    BufferPage(int size, boolean direct) {
        freeList = new LinkedList<>();
        if (size > 0) {
            this.buffer = allocate0(size, direct);
            freeList.add(new VirtualBuffer(this, null, buffer.position(), buffer.limit()));
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

    public synchronized VirtualBuffer allocate(final int size) {
        Iterator<VirtualBuffer> iterator = freeList.iterator();
        VirtualBuffer bufferChunk = null;
        while (iterator.hasNext()) {
            VirtualBuffer freeChunk = iterator.next();
            final int remaining = freeChunk.getParentLimit() - freeChunk.getParentPosition();
            if (remaining < size) {
                continue;
            }
            if (remaining == size) {
                iterator.remove();
                buffer.limit(freeChunk.getParentLimit());
                buffer.position(freeChunk.getParentPosition());
                freeChunk.buffer(buffer.slice());
                bufferChunk = freeChunk;
            } else {
                buffer.limit(freeChunk.getParentPosition() + size);
                buffer.position(freeChunk.getParentPosition());
                bufferChunk = new VirtualBuffer(this, buffer.slice(), buffer.position(), buffer.limit());
                freeChunk.setParentPosition(buffer.limit());
            }
            if (bufferChunk.buffer().remaining() != size) {
                LOGGER.error(bufferChunk.buffer().remaining() + "aaaa" + size);
                throw new RuntimeException("allocate " + size + ", buffer:" + bufferChunk);
            }
            return bufferChunk;
        }
        LOGGER.warn("bufferPage has no available space: " + size);
        return new VirtualBuffer(null, allocate0(size, false), 0, 0);
    }

    synchronized void clean(VirtualBuffer cleanBuffer) {
        if (freeList.isEmpty()) {
            freeList.add(cleanBuffer);
            return;
        }
        int index = 0;
        Iterator<VirtualBuffer> iterator = freeList.iterator();
        while (iterator.hasNext()) {
            VirtualBuffer freeBuffer = iterator.next();
            //cleanBuffer在freeBuffer之前并且形成连续块
            if (freeBuffer.getParentPosition() == cleanBuffer.getParentLimit()) {
                freeBuffer.setParentPosition(cleanBuffer.getParentPosition());
                return;
            }
            //cleanBuffer与freeBuffer之后并形成连续块
            if (freeBuffer.getParentLimit() == cleanBuffer.getParentPosition()) {
                freeBuffer.setParentLimit(cleanBuffer.getParentLimit());
                //判断后一个是否连续
                if (iterator.hasNext()) {
                    VirtualBuffer next = iterator.next();
                    if (next.getParentPosition() == freeBuffer.getParentLimit()) {
                        freeBuffer.setParentLimit(next.getParentLimit());
                        iterator.remove();
                    } else if (next.getParentPosition() < freeBuffer.getParentLimit()) {
                        throw new IllegalStateException("");
                    }
                }
                return;
            }
            if (freeBuffer.getParentPosition() > cleanBuffer.getParentLimit()) {
                freeList.add(index, cleanBuffer);
                return;
            }
            index++;
        }
        freeList.add(cleanBuffer);
    }
}
