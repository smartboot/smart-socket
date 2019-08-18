package org.smartboot.socket.udp;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
final class WriteEvent {
    /**
     * 待输出数据的接受地址
     */
    private SocketAddress remote;
    /**
     * 待输出数据
     */
    private ByteBuffer buffer;

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public SocketAddress getRemote() {
        return remote;
    }

    public void setRemote(SocketAddress remote) {
        this.remote = remote;
    }
}
