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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/25
 */
final class UdpAioSession extends AioSession {

    private final UdpChannel udpChannel;

    private final SocketAddress remote;

    private final WriteBuffer writeBuffer;

    UdpAioSession(final UdpChannel udpChannel, final SocketAddress remote, WriteBuffer writeBuffer) {
        this.udpChannel = udpChannel;
        this.remote = remote;
        this.writeBuffer = writeBuffer;
        udpChannel.config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);
    }

    @Override
    public WriteBuffer writeBuffer() {
        return writeBuffer;
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
        udpChannel.removeSession(remote);
    }

    @Override
    public InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) udpChannel.getChannel().getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() throws IOException {
        return (InetSocketAddress) remote;
    }
}
