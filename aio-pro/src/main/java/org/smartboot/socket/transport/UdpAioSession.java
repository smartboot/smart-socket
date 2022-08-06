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
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/25
 */
final class UdpAioSession extends AioSession {

    private final UdpChannel udpChannel;

    private final SocketAddress remote;

    private final WriteBuffer writeBuffer;

    UdpAioSession(final UdpChannel udpChannel, final SocketAddress remote, BufferPage bufferPage) {
        this.udpChannel = udpChannel;
        this.remote = remote;
        Consumer<WriteBuffer> consumer = writeBuffer -> {
            VirtualBuffer virtualBuffer = writeBuffer.poll();
            if (virtualBuffer == null) {
                return;
            }
            try {
                udpChannel.write(virtualBuffer, UdpAioSession.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        this.writeBuffer = new WriteBuffer(bufferPage, consumer, udpChannel.config.getWriteBufferSize(), 1);
        udpChannel.config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);
    }

    @Override
    public WriteBuffer writeBuffer() {
        return writeBuffer;
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

    @Override
    public void close(boolean immediate) {
        writeBuffer.close();
        udpChannel.config.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
    }

    @Override
    public InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) udpChannel.getChannel().getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) remote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UdpAioSession that = (UdpAioSession) o;

        return Objects.equals(remote, that.remote);
    }

    @Override
    public int hashCode() {
        return remote != null ? remote.hashCode() : 0;
    }
}
