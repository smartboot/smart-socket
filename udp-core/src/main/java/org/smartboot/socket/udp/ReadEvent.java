package org.smartboot.socket.udp;

import java.net.SocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
final class ReadEvent<T> {
    /**
     * 当前收到消息的UDP通道
     */
    private UdpChannel<T> channel;
    /**
     * 消息来源
     */
    private SocketAddress remote;
    /**
     * 消息体
     */
    private T message;

    public SocketAddress getRemote() {
        return remote;
    }

    public void setRemote(SocketAddress remote) {
        this.remote = remote;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    public UdpChannel<T> getChannel() {
        return channel;
    }

    public void setChannel(UdpChannel<T> channel) {
        this.channel = channel;
    }
}
