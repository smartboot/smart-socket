/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BufferPage.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.buffer;


import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ByteBuffer内存页类，负责管理和分配ByteBuffer内存。
 * 该类实现了一个内存池的基本单元，通过复用ByteBuffer对象来减少内存分配和GC压力。
 * 内存页可以管理堆内存或堆外内存（直接缓冲区），并提供内存分配、回收和释放功能。
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
final class BufferPage {
    /**
     * 内存页是否处于空闲状态。
     * 当值为true时，表示当前内存页处于空闲状态，可以进行内存回收；
     * 当值为false时，表示当前内存页正在被使用。
     */
    private boolean idle = true;
    /**
     * 待回收的虚拟Buffer队列。
     * 使用线程安全的并发队列存储已被使用完毕但尚未释放的VirtualBuffer对象，
     * 这些对象可以被重新利用，从而减少内存分配和GC压力。
     */
    private final ConcurrentLinkedQueue<VirtualBuffer> cleanBuffers = new ConcurrentLinkedQueue<>();

    /**
     * 标识该内存页是否使用堆外内存（直接缓冲区）。
     * 如果值为 true，表示使用堆外内存，即使用 ByteBuffer.allocateDirect() 分配内存；
     * 如果值为 false，表示使用堆内内存，即使用 ByteBuffer.allocate() 分配内存。
     * 该属性在对象创建后不可修改。
     */
    private final boolean direct;

    /**
     * 构造一个内存页对象。
     *
     * @param direct 是否使用堆外内存。
     *               当值为true时，使用ByteBuffer.allocateDirect()分配直接内存；
     *               当值为false时，使用ByteBuffer.allocate()分配堆内存。
     */
    BufferPage(boolean direct) {
        this.direct = direct;
    }


    /**
     * 申请指定大小的虚拟内存。
     * 该方法首先尝试从回收队列中获取一个合适大小的VirtualBuffer对象进行复用，
     * 如果没有找到合适的，则创建一个新的VirtualBuffer对象。
     *
     * @param size 申请的内存大小（字节数）
     * @return 分配的虚拟内存对象
     * @throws UnsupportedOperationException 当申请大小为0时抛出异常
     */
    public VirtualBuffer allocate(final int size) {
        if (size == 0) {
            throw new UnsupportedOperationException("cannot allocate zero bytes");
        }
        // 标记内存页为非空闲状态
        idle = false;
        // 尝试从回收队列中获取一个VirtualBuffer
        VirtualBuffer virtualBuffer = cleanBuffers.poll();
        // 如果找到了一个容量正好等于请求大小的VirtualBuffer，则重置并返回它
        if (virtualBuffer != null && virtualBuffer.buffer().capacity() == size) {
            virtualBuffer.reset();
            return virtualBuffer;
        }

        // 如果找到了VirtualBuffer但大小不匹配，则释放它
        if (virtualBuffer != null) {
            clean0(virtualBuffer);
        }
        // 创建一个新的VirtualBuffer，根据direct标志决定使用直接缓冲区还是堆缓冲区
        return new VirtualBuffer(this, direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size));
    }


    /**
     * 回收虚拟内存到回收队列中，使其可以被后续的allocate调用复用。
     * 该方法不会立即释放内存，而是将VirtualBuffer对象放入回收队列中等待复用。
     *
     * @param cleanBuffer 待回收的虚拟内存对象
     */
    public void clean(VirtualBuffer cleanBuffer) {
        cleanBuffers.offer(cleanBuffer);
    }

    /**
     * 尝试回收缓冲区。
     * 该方法由定时任务调用，用于定期检查和回收不再需要的缓冲区。
     * 采用两阶段回收策略：第一次调用时标记内存页为空闲状态，第二次连续调用时才真正回收内存。
     * 这种策略可以避免频繁地分配和释放内存，提高性能。
     */
    public void tryClean() {
        // 如果内存页当前不是空闲状态，则标记为空闲并等待下一个周期
        if (!idle) {
            idle = true;
        } else {
            // 如果内存页已经连续两个周期处于空闲状态，则开始回收内存
            // 每次最多回收10个缓冲区，避免一次性回收过多导致性能问题
            int count = 0;
            VirtualBuffer cleanBuffer;
            while (idle && count++ < 10 && (cleanBuffer = cleanBuffers.poll()) != null) {
                clean0(cleanBuffer);
            }
        }
    }

    /**
     * 真正回收虚拟缓冲区的底层资源。
     * 对于直接缓冲区(DirectBuffer)，调用其cleaner的clean方法释放本地内存；
     * 对于堆内存缓冲区，不需要特殊处理，依靠Java的GC机制回收。
     *
     * @param virtualBuffer 需要回收的虚拟缓冲区
     */
    private void clean0(VirtualBuffer virtualBuffer) {
        // 只有直接缓冲区需要显式释放
        if (direct) {
            try {
                // 调用DirectBuffer的cleaner来释放本地内存
                ((DirectBuffer) virtualBuffer.buffer()).cleaner().clean();
            } catch (Throwable e) {
                // 捕获并打印可能出现的异常，但不中断程序执行
                e.printStackTrace();
            }
        }
        // 对于堆内存缓冲区，不需要特殊处理，由GC自动回收
    }

    /**
     * 释放该内存页管理的所有内存资源。
     * 该方法通常在系统关闭或内存页不再使用时调用，用于彻底释放所有资源。
     * 对于直接缓冲区，会遍历回收队列中的所有VirtualBuffer并释放它们的本地内存。
     */
    public void release() {
        // 只有直接缓冲区需要显式释放
        if (direct) {
            VirtualBuffer virtualBuffer;
            // 遍历并释放回收队列中的所有VirtualBuffer
            while ((virtualBuffer = cleanBuffers.poll()) != null) {
                clean0(virtualBuffer);
            }
        }
        // 对于堆内存缓冲区，不需要特殊处理，由GC自动回收
    }

    /**
     * 返回该内存页的字符串表示形式，用于调试和日志记录。
     *
     * @return 包含内存页类型和可用缓冲区信息的字符串
     */
    @Override
    public String toString() {
        return "BufferPage{direct=" + direct + " ,availableBuffers=" + cleanBuffers + '}';
    }
}
