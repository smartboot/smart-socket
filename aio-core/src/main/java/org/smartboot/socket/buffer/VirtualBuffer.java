/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: VirtualBuffer.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

/**
 * 虚拟ByteBuffer缓冲区
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public final class VirtualBuffer {

    /**
     * 当前虚拟buffer的归属内存页
     */
    private final BufferPage bufferPage;
    /**
     * 通过ByteBuffer.slice()隐射出来的虚拟ByteBuffer
     *
     * @see ByteBuffer#slice()
     */
    private ByteBuffer buffer;
    /**
     * 是否已回收
     */
    private final Semaphore clean = new Semaphore(1);
    /**
     * 当前虚拟buffer映射的实际buffer.position
     */
    private int parentPosition;

    /**
     * 当前虚拟buffer映射的实际buffer.limit
     */
    private int parentLimit;

    /**
     * 缓冲区容量
     */
    private int capacity;

    VirtualBuffer(BufferPage bufferPage, ByteBuffer buffer, int parentPosition, int parentLimit) {
        this.bufferPage = bufferPage;
        this.buffer = buffer;
        this.parentPosition = parentPosition;
        this.parentLimit = parentLimit;
        updateCapacity();
    }

    public static VirtualBuffer wrap(ByteBuffer buffer) {
        return new VirtualBuffer(null, buffer, 0, 0);
    }

    int getParentPosition() {
        return parentPosition;
    }

    void setParentPosition(int parentPosition) {
        this.parentPosition = parentPosition;
        updateCapacity();
    }

    int getParentLimit() {
        return parentLimit;
    }

    void setParentLimit(int parentLimit) {
        this.parentLimit = parentLimit;
        updateCapacity();
    }

    private void updateCapacity() {
        capacity = this.parentLimit - this.parentPosition;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * 获取真实缓冲区
     *
     * @return 真实缓冲区
     */
    public ByteBuffer buffer() {
        return buffer;
    }

    /**
     * 设置真实缓冲区
     *
     * @param buffer 真实缓冲区
     */
    void buffer(ByteBuffer buffer) {
        this.buffer = buffer;
        clean.release();
    }

    /**
     * 释放虚拟缓冲区
     */
    public void clean() {
        if (clean.tryAcquire()) {
            if (bufferPage != null) {
                bufferPage.clean(this);
            }
        } else {
            throw new UnsupportedOperationException("buffer has cleaned");
        }
    }

    @Override
    public String toString() {
        return "VirtualBuffer{parentPosition=" + parentPosition + ", parentLimit=" + parentLimit + '}';
    }
}
