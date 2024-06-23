/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpAioSession.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/25
 */
final class UdpAioSession extends AioSession {

    private final UdpChannel udpChannel;

    private final SocketAddress remote;

    private final WriteBuffer byteBuf;

    UdpAioSession(final UdpChannel udpChannel, final SocketAddress remote, BufferPage writeBufferPage) {
        this.udpChannel = udpChannel;
        this.remote = remote;
        this.byteBuf = new WriteBuffer(writeBufferPage, buffer -> udpChannel.write(buffer, UdpAioSession.this), udpChannel.config.getWriteBufferSize(), 1);
        udpChannel.config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);
    }

    @Override
    public WriteBuffer writeBuffer() {
        return byteBuf;
    }

    @Override
    public ByteBuffer readBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void awaitRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void signalRead() {
        throw new UnsupportedOperationException();
    }

    /**
     * 为确保消息尽可能发送，UDP不支持立即close
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    @Override
    public void close(boolean immediate) {
        byteBuf.flush();
    }

    @Override
    public InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) udpChannel.getChannel().getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) remote;
    }

}
