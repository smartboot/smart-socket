/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SSLAioSession.java
 * Date: 2017-12-19 15:01:29
 * Author: sandao
 */

package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2017/12/19
 */
public class SSLAioSession<T> extends AioSession<T> {
    private static final Logger logger = LogManager.getLogger(SSLAioSession.class);
    private ByteBuffer netWriteBuffer;

    private ByteBuffer netReadBuffer;
    private SSLEngine sslEngine = null;

    /**
     * @param channel
     * @param config
     * @param aioReadCompletionHandler
     * @param aioWriteCompletionHandler
     * @param serverSession             是否服务端Session
     */
    SSLAioSession(AsynchronousSocketChannel channel, IoServerConfig<T> config, ReadCompletionHandler aioReadCompletionHandler, WriteCompletionHandler aioWriteCompletionHandler, boolean serverSession) {
        super(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, serverSession);
    }

    @Override
    void writeToChannel() {
        if (netWriteBuffer != null && netWriteBuffer.hasRemaining()) {
            writeToChannel0(netWriteBuffer);
            return;
        }
        super.writeToChannel();
    }

    public void initSession() {
        throw new UnsupportedOperationException("please call method [initSession(SSLEngine sslEngine)]");
    }

    public void initSession(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
        this.netWriteBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        this.netWriteBuffer.flip();
        this.netReadBuffer = ByteBuffer.allocate(readBuffer.capacity());
        readFromChannel(false);
    }

    @Override
    void readFromChannel(boolean eof) {
        doUnWrap();
        super.readFromChannel(eof);
    }

    @Override
    protected void continueRead() {
        readFromChannel0(netReadBuffer);
    }

    @Override
    protected void continueWrite() {
        doWrap();
        writeToChannel0(netWriteBuffer);
    }

    private void doWrap() {
        try {
            netWriteBuffer.compact();
            SSLEngineResult result = sslEngine.wrap(writeBuffer, netWriteBuffer);
            if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
                System.exit(-1);
            }
            netWriteBuffer.flip();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private void doUnWrap() {
        try {
            netReadBuffer.flip();
            SSLEngineResult result = sslEngine.unwrap(netReadBuffer, readBuffer);
            logger.info(result);
            netReadBuffer.compact();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

}
