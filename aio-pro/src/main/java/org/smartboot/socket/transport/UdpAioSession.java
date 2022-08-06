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
import org.smartboot.socket.buffer.VirtualBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/25
 */
final class UdpAioSession extends AioSession {

    private final UdpChannel udpChannel;

    private final SocketAddress remote;

    private final WriteBuffer byteBuf;
    /**
     * 写缓冲
     */
    private VirtualBuffer writeBuffer;

    UdpAioSession(final UdpChannel udpChannel, final SocketAddress remote, BufferPage bufferPage) {
        this.udpChannel = udpChannel;
        this.remote = remote;
        Consumer<WriteBuffer> consumer = var -> {
            UdpAioSession.this.writeBuffer = var.poll();
            if (writeBuffer != null) {
                try {
                    udpChannel.write(writeBuffer, UdpAioSession.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        this.byteBuf = new WriteBuffer(bufferPage, consumer, udpChannel.config.getWriteBufferSize(), 1);
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
        if (status == SESSION_STATUS_CLOSED) {
            return;
        }
        if ((writeBuffer == null || !writeBuffer.buffer().hasRemaining()) && byteBuf.isEmpty()) {
            status = SESSION_STATUS_CLOSED;
            try {
                byteBuf.close();
            } finally {
                udpChannel.config.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
            }
        } else {
            udpChannel.config.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSING, null);
            byteBuf.flush();
        }
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
