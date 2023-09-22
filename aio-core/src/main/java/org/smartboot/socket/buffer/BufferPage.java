/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BufferPage.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.buffer;


import sun.misc.Unsafe;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ByteBuffer内存页
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public final class BufferPage {
    /**
     * 同组内存池中的各内存页
     */
    private final BufferPage[] poolPages;
    /**
     * 条件锁
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * 当前缓存页的物理缓冲区
     */
    private final ByteBuffer buffer;
    /**
     * 待回收的虚拟Buffer
     */
    private final ConcurrentLinkedQueue<VirtualBuffer> cleanBuffers = new ConcurrentLinkedQueue<>();
    /**
     * 当前空闲的虚拟Buffer
     */
    private final List<VirtualBuffer> availableBuffers;
    /**
     * 内存页是否处于空闲状态
     */
    private boolean idle = true;

    /**
     * @param size   缓存页大小
     * @param direct 是否使用堆外内存
     */
    BufferPage(BufferPage[] poolPages, int size, boolean direct) {
        this.poolPages = Objects.requireNonNull(poolPages);
        availableBuffers = new LinkedList<>();
        this.buffer = allocate0(size, direct);
        availableBuffers.add(new VirtualBuffer(this, null, buffer.position(), buffer.limit()));
    }

    /**
     * 申请物理内存页空间
     *
     * @param size   物理空间大小
     * @param direct true:堆外缓冲区,false:堆内缓冲区
     * @return 缓冲区
     */
    private ByteBuffer allocate0(int size, boolean direct) {
        return direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }

    /**
     * 申请虚拟内存
     *
     * @param size 申请大小
     * @return 虚拟内存对象
     */
    public VirtualBuffer allocate(final int size) {
        if (size == 0) {
            throw new UnsupportedOperationException("cannot allocate zero bytes");
        }
        VirtualBuffer virtualBuffer;
        Thread thread = Thread.currentThread();
        if (thread instanceof FastBufferThread) {
            FastBufferThread fastBufferThread = (FastBufferThread) thread;
            virtualBuffer = fastBufferThread.getPageIndex() < poolPages.length ? poolPages[fastBufferThread.getPageIndex()].allocate0(size) : allocate0(size);
        } else {
            virtualBuffer = allocate0(size);
        }
        return virtualBuffer == null ? new VirtualBuffer(null, allocate0(size, false), 0, 0) : virtualBuffer;
    }

    /**
     * 申请虚拟内存
     *
     * @param size 申请大小
     * @return 虚拟内存对象
     */
    private VirtualBuffer allocate0(final int size) {
        idle = false;
        VirtualBuffer cleanBuffer = cleanBuffers.poll();
        if (cleanBuffer != null && cleanBuffer.getCapacity() >= size) {
            cleanBuffer.buffer().clear();
            cleanBuffer.buffer(cleanBuffer.buffer());
            return cleanBuffer;
        }
        lock.lock();
        try {
            if (cleanBuffer != null) {
                clean0(cleanBuffer);
                while ((cleanBuffer = cleanBuffers.poll()) != null) {
                    if (cleanBuffer.getCapacity() >= size) {
                        cleanBuffer.buffer().clear();
                        cleanBuffer.buffer(cleanBuffer.buffer());
                        return cleanBuffer;
                    } else {
                        clean0(cleanBuffer);
                    }
                }
            }

            int count = availableBuffers.size();
            VirtualBuffer bufferChunk = null;
            //仅剩一个可用内存块的时候使用快速匹配算法
            if (count == 1) {
                bufferChunk = fastAllocate(size);
            } else if (count > 1) {
                bufferChunk = slowAllocate(size);
            }
            return bufferChunk;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 快速匹配
     *
     * @param size 申请内存大小
     * @return 申请到的内存块, 若空间不足则返回null
     */
    private VirtualBuffer fastAllocate(int size) {
        VirtualBuffer freeChunk = availableBuffers.get(0);
        VirtualBuffer bufferChunk = allocate(size, freeChunk);
        if (freeChunk == bufferChunk) {
            availableBuffers.clear();
        }
        return bufferChunk;
    }

    /**
     * 迭代申请
     *
     * @param size 申请内存大小
     * @return 申请到的内存块, 若空间不足则返回null
     */
    private VirtualBuffer slowAllocate(int size) {
        Iterator<VirtualBuffer> iterator = availableBuffers.listIterator(0);
        VirtualBuffer bufferChunk;
        while (iterator.hasNext()) {
            VirtualBuffer freeChunk = iterator.next();
            bufferChunk = allocate(size, freeChunk);
            if (freeChunk == bufferChunk) {
                iterator.remove();
            }
            if (bufferChunk != null) {
                return bufferChunk;
            }
        }
        return null;
    }

    /**
     * 从可用内存大块中申请所需的内存小块
     *
     * @param size      申请内存大小
     * @param freeChunk 可用于申请的内存块
     * @return 申请到的内存块, 若空间不足则返回null
     */
    private VirtualBuffer allocate(int size, VirtualBuffer freeChunk) {
        final int capacity = freeChunk.getCapacity();
        if (capacity < size) {
            return null;
        }
        VirtualBuffer bufferChunk;
        if (capacity == size) {
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
            throw new RuntimeException("allocate " + size + ", buffer:" + bufferChunk);
        }
        return bufferChunk;
    }


    /**
     * 内存回收
     *
     * @param cleanBuffer 待回收的虚拟内存
     */
    void clean(VirtualBuffer cleanBuffer) {
        cleanBuffers.offer(cleanBuffer);
    }

    /**
     * 尝试回收缓冲区
     */
    void tryClean() {
        //下个周期依旧处于空闲则触发回收任务
        if (!idle) {
            idle = true;
        } else if (!cleanBuffers.isEmpty() && lock.tryLock()) {
            try {
                VirtualBuffer cleanBuffer;
                while ((cleanBuffer = cleanBuffers.poll()) != null) {
                    clean0(cleanBuffer);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 回收虚拟缓冲区
     *
     * @param cleanBuffer 虚拟缓冲区
     */
    private void clean0(VirtualBuffer cleanBuffer) {
        ListIterator<VirtualBuffer> iterator = availableBuffers.listIterator(0);
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
                iterator.previous();
                iterator.add(cleanBuffer);
                return;
            }
        }
        iterator.add(cleanBuffer);
    }

    /**
     * 释放内存
     */
    void release() {
        if (buffer.isDirect()) {
            Unsafe.getUnsafe().invokeCleaner(buffer);
        }
    }

    @Override
    public String toString() {
        return "BufferPage{availableBuffers=" + availableBuffers + ", cleanBuffers=" + cleanBuffers + '}';
    }
}
