package org.smartboot.socket.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/25
 */
final class UdpAioSession<T> extends AioSession<T> {

    private UdpChannel udpChannel;

    private SocketAddress remote;

    private WriteBuffer writeBuffer;

    UdpAioSession(final UdpChannel udpChannel, final SocketAddress remote, WriteBuffer writeBuffer) {
        this.udpChannel = udpChannel;
        this.remote = remote;
        this.writeBuffer = writeBuffer;
    }

    @Override
    public WriteBuffer writeBuffer() {
        return writeBuffer;
    }

    @Override
    public void close(boolean immediate) {
        writeBuffer.close();
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
