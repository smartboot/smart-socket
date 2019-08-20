package org.smartboot.socket.udp;

import java.net.SocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
final class WriteEvent<Response> {
    /**
     * 待输出数据的接受地址
     */
    private SocketAddress remote;
    /**
     * 待输出数据
     */
    private Response response;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public SocketAddress getRemote() {
        return remote;
    }

    public void setRemote(SocketAddress remote) {
        this.remote = remote;
    }
}
