package org.smartboot.socket.udp;

import java.net.SocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
final class ReadEvent<Request, Response> {
    /**
     * 当前收到消息的UDP通道
     */
    private UdpChannel<Request, Response> channel;
    /**
     * 消息来源
     */
    private SocketAddress remote;
    /**
     * 消息体
     */
    private Request message;

    public SocketAddress getRemote() {
        return remote;
    }

    public void setRemote(SocketAddress remote) {
        this.remote = remote;
    }

    public Request getMessage() {
        return message;
    }

    public void setMessage(Request message) {
        this.message = message;
    }

    public UdpChannel<Request, Response> getChannel() {
        return channel;
    }

    public void setChannel(UdpChannel<Request, Response> channel) {
        this.channel = channel;
    }
}
