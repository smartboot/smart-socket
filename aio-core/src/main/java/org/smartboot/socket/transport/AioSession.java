/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioSession.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/25
 */
public abstract class AioSession {


    /**
     * Session状态:已关闭
     */
    protected static final byte SESSION_STATUS_CLOSED = 1;
    /**
     * Session状态:关闭中
     */
    protected static final byte SESSION_STATUS_CLOSING = 2;
    /**
     * Session状态:正常
     */
    protected static final byte SESSION_STATUS_ENABLED = 3;


    /**
     * 会话当前状态
     *
     * @see AioSession#SESSION_STATUS_CLOSED
     * @see AioSession#SESSION_STATUS_CLOSING
     * @see AioSession#SESSION_STATUS_ENABLED
     */
    protected byte status = SESSION_STATUS_ENABLED;
    /**
     * 是否读通道以至末尾
     */
    boolean eof;
    int modCount = 0;
    /**
     * 附件对象
     */
    private Object attachment;

    /**
     * 获取WriteBuffer用以数据输出
     *
     * @return WriteBuffer
     */
    public abstract WriteBuffer writeBuffer();

    /**
     * 获取读缓冲区对象
     */
    public abstract ByteBuffer readBuffer();

    /**
     * 强制关闭当前AIOSession。
     * <p>若此时还存留待输出的数据，则会导致该部分数据丢失</p>
     */
    public final void close() {
        close(true);
    }

    public abstract void awaitRead();

    /**
     * 继续触发读行为，该方法仅可在异步处理模式下可使用，否则会触发不可预知的异常
     */
    public abstract void signalRead();

    /**
     * 是否立即关闭会话
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    public abstract void close(boolean immediate);

    /**
     * 获取当前Session的唯一标识
     *
     * @return sessionId
     */
    public String getSessionID() {
        return "aioSession-" + hashCode();
    }

    /**
     * 当前会话是否已失效
     *
     * @return 是否失效
     */
    public boolean isInvalid() {
        return status != SESSION_STATUS_ENABLED;
    }


    /**
     * 获取附件对象
     *
     * @param <A> 附件对象类型
     * @return 附件
     */
    public final <A> A getAttachment() {
        return (A) attachment;
    }

    /**
     * 存放附件，支持任意类型
     *
     * @param <A>        附件对象类型
     * @param attachment 附件对象
     */
    public final <A> void setAttachment(A attachment) {
        this.attachment = attachment;
    }

    /**
     * 获取当前会话的本地连接地址
     *
     * @return 本地地址
     * @throws IOException IO异常
     * @see AsynchronousSocketChannel#getLocalAddress()
     */
    public abstract InetSocketAddress getLocalAddress() throws IOException;

    /**
     * 获取当前会话的远程连接地址
     *
     * @return 远程地址
     * @throws IOException IO异常
     * @see AsynchronousSocketChannel#getRemoteAddress()
     */
    public abstract InetSocketAddress getRemoteAddress() throws IOException;

    //执行同步读操作
    public final int syncRead() throws IOException {
        return syncRead(0, TimeUnit.MILLISECONDS);
    }

    //执行同步读操作
    public int syncRead(long timeout, TimeUnit unit) throws IOException {
        throw new UnsupportedOperationException();
    }
}
