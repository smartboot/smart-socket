/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HandshakeModel.java
 * Date: 2018-01-02 11:29:49
 * Author: sandao
 */

package org.smartboot.socket.extension.ssl;

import javax.net.ssl.SSLEngine;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/2
 */
public class HandshakeModel {

    private AsynchronousSocketChannel socketChannel;
    private SSLEngine sslEngine;
    private ByteBuffer appWriteBuffer;
    private ByteBuffer netWriteBuffer;
    private ByteBuffer appReadBuffer;

    private ByteBuffer netReadBuffer;
    private boolean finished;

    public AsynchronousSocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(AsynchronousSocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public ByteBuffer getAppWriteBuffer() {
        return appWriteBuffer;
    }

    public void setAppWriteBuffer(ByteBuffer appWriteBuffer) {
        this.appWriteBuffer = appWriteBuffer;
    }

    public ByteBuffer getNetWriteBuffer() {
        return netWriteBuffer;
    }

    public void setNetWriteBuffer(ByteBuffer netWriteBuffer) {
        this.netWriteBuffer = netWriteBuffer;
    }

    public ByteBuffer getAppReadBuffer() {
        return appReadBuffer;
    }

    public void setAppReadBuffer(ByteBuffer appReadBuffer) {
        this.appReadBuffer = appReadBuffer;
    }

    public ByteBuffer getNetReadBuffer() {
        return netReadBuffer;
    }

    public void setNetReadBuffer(ByteBuffer netReadBuffer) {
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
}
