/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HandshakeModel.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;

import org.smartboot.socket.buffer.VirtualBuffer;

import javax.net.ssl.SSLEngine;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/2
 */
public class HandshakeModel {

    private AsynchronousSocketChannel socketChannel;
    private SSLEngine sslEngine;
    private VirtualBuffer appWriteBuffer;
    private VirtualBuffer netWriteBuffer;
    private VirtualBuffer appReadBuffer;

    private VirtualBuffer netReadBuffer;
    private HandshakeCompletionHandler handshakeCompletionHandler;
    private boolean finished;

    public AsynchronousSocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(AsynchronousSocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public VirtualBuffer getAppWriteBuffer() {
        return appWriteBuffer;
    }

    public void setAppWriteBuffer(VirtualBuffer appWriteBuffer) {
        this.appWriteBuffer = appWriteBuffer;
    }

    public VirtualBuffer getNetWriteBuffer() {
        return netWriteBuffer;
    }

    public void setNetWriteBuffer(VirtualBuffer netWriteBuffer) {
        this.netWriteBuffer = netWriteBuffer;
    }

    public VirtualBuffer getAppReadBuffer() {
        return appReadBuffer;
    }

    public void setAppReadBuffer(VirtualBuffer appReadBuffer) {
        this.appReadBuffer = appReadBuffer;
    }

    public VirtualBuffer getNetReadBuffer() {
        return netReadBuffer;
    }

    public void setNetReadBuffer(VirtualBuffer netReadBuffer) {
        this.netReadBuffer = netReadBuffer;
    }

    public SSLEngine getSslEngine() {
        return sslEngine;
    }

    public void setSslEngine(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public HandshakeCompletionHandler getHandshakeCompletionHandler() {
        return handshakeCompletionHandler;
    }

    public void setHandshakeCompletionHandler(HandshakeCompletionHandler handshakeCompletionHandler) {
        this.handshakeCompletionHandler = handshakeCompletionHandler;
    }
}
