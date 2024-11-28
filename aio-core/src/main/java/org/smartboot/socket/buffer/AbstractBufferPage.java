package org.smartboot.socket.buffer;

abstract class AbstractBufferPage implements BufferPage {
    /**
     * 内存页是否处于空闲状态
     */
    protected boolean idle = true;

    /**
     * 内存回收
     *
     * @param cleanBuffer 待回收的虚拟内存
     */
    abstract void clean(VirtualBuffer cleanBuffer);

    /**
     * 尝试回收缓冲区
     */
    abstract void tryClean();


    /**
     * 释放内存
     */
    abstract void release();
}
