/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpWriteEvent.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.smartboot.socket.buffer.VirtualBuffer;

import java.net.SocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
final class UdpWriteEvent {
    /**
     * 待输出数据的接受地址
     */
    private SocketAddress remote;
    /**
     * 待输出数据
     */
    private VirtualBuffer response;

    public VirtualBuffer getResponse() {
        return response;
    }

    public void setResponse(VirtualBuffer response) {
        this.response = response;
    }

    public SocketAddress getRemote() {
        return remote;
    }

    public void setRemote(SocketAddress remote) {
        this.remote = remote;
    }
}
