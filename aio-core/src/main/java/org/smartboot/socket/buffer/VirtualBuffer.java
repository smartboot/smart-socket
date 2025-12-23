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

/**
 * 虚拟ByteBuffer缓冲区类，对Java NIO的ByteBuffer进行封装。
 * 该类实现了对ByteBuffer的池化管理，通过与BufferPage关联，
 * 实现了缓冲区的复用和资源回收，减少内存分配和GC压力。
 * 提供了获取底层缓冲区、重置和清理等操作，使ByteBuffer的使用更加高效和安全。
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public final class VirtualBuffer {

    /**
     * 当前虚拟buffer的归属内存页。
     * 如果该值不为null，表示这个VirtualBuffer是由BufferPage创建和管理的，
     * 当不再使用时，会被回收到对应的BufferPage中；
     * 如果该值为null，表示这个VirtualBuffer是通过wrap方法创建的，
     * 不受内存池管理，不会被回收复用。
     */
    private final BufferPage bufferPage;

    private boolean enable = true;
    /**
     * 底层的ByteBuffer实例，所有的读写操作最终都会转发到这个缓冲区。
     * 这个缓冲区可能是堆内存缓冲区(HeapByteBuffer)或直接缓冲区(DirectByteBuffer)，
     * 具体类型由创建VirtualBuffer的BufferPage决定。
     *
     * @see ByteBuffer
     * @see java.nio.HeapByteBuffer
     * @see sun.nio.ch.DirectBuffer
     */
    private final ByteBuffer buffer;


    /**
     * 构造一个虚拟缓冲区对象。
     * 该构造函数为包级私有，只能由同包中的类调用，通常由BufferPage在分配内存时调用。
     *
     * @param bufferPage 归属的内存页，如果为null表示不受内存池管理
     * @param buffer     底层的ByteBuffer实例，所有操作都会转发到这个缓冲区
     */
    VirtualBuffer(BufferPage bufferPage, ByteBuffer buffer) {
        this.bufferPage = bufferPage;
        this.buffer = buffer;
    }

    /**
     * 将现有的ByteBuffer包装成VirtualBuffer。
     * 通过这种方式创建的VirtualBuffer不受内存池管理，不会被回收复用。
     * 这个方法通常用于将外部传入的ByteBuffer转换为VirtualBuffer，以便统一接口。
     *
     * @param buffer 要包装的ByteBuffer实例
     * @return 包装后的VirtualBuffer实例
     */
    public static VirtualBuffer wrap(ByteBuffer buffer) {
        return new VirtualBuffer(null, buffer);
    }


    /**
     * 获取底层的ByteBuffer实例。
     * 调用者可以直接操作返回的ByteBuffer，进行读写等操作。
     * 注意：在使用完毕后，应该调用VirtualBuffer的clean()方法而不是直接释放ByteBuffer，
     * 以确保资源能够正确回收。
     *
     * @return 底层的ByteBuffer实例
     */
    public ByteBuffer buffer() {
        return buffer;
    }

    /**
     * 重置缓冲区状态，准备复用。
     * 该方法会清空底层ByteBuffer的内容，并重置信号量状态，使VirtualBuffer可以再次使用。
     * 这个方法为包级私有，通常由BufferPage在复用VirtualBuffer时调用。
     *
     * @see ByteBuffer#clear()
     */
    void reset() {
        // 清空底层ByteBuffer的内容，重置position和limit
        this.buffer.clear();
        // 重置信号量，使VirtualBuffer可以再次被清理
        enable = true;
    }

    /**
     * 释放虚拟缓冲区资源。
     * 如果该VirtualBuffer是由BufferPage创建的，则会将其归还给对应的BufferPage进行复用；
     * 如果是通过wrap方法创建的，则不做特殊处理。
     * 该方法使用信号量确保每个VirtualBuffer只能被清理一次，防止重复清理。
     *
     * @throws UnsupportedOperationException 如果尝试重复清理同一个VirtualBuffer
     */
    public synchronized void clean() {
        // 尝试获取信号量，如果获取成功表示这是第一次清理
        if (enable) {
            enable = false;
            // 如果有关联的BufferPage，则将自己归还给BufferPage
            if (bufferPage != null) {
                bufferPage.clean(this);
            }
            // 如果没有关联的BufferPage（通过wrap方法创建），则不做特殊处理
        } else {
            // 如果获取信号量失败，表示已经被清理过，抛出异常
            throw new UnsupportedOperationException("buffer has cleaned");
        }
    }

    /**
     * 返回该虚拟缓冲区的字符串表示形式，用于调试和日志记录。
     * 直接返回底层ByteBuffer的字符串表示形式。
     *
     * @return 底层ByteBuffer的字符串表示形式
     */
    @Override
    public String toString() {
        return buffer.toString();
    }
}
