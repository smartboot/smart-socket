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
            if (!netReadBuffer.hasRemaining()) {
                logger.info("no remain");
                netReadBuffer.compact();
                return;
            }

//            logger.info("first:" + netReadBuffer + " " + readBuffer);

            SSLEngineResult result = sslEngine.unwrap(netReadBuffer, readBuffer);
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        // Could attempt to drain the dst buffer of any already obtained
                        // data, but we'll just increase it to the size needed.
                        int appSize = sslEngine.getSession().getApplicationBufferSize();
                        ByteBuffer b = ByteBuffer.allocate(appSize + readBuffer.position());
                        readBuffer.flip();
                        b.put(readBuffer);
                        readBuffer = b;
                        // retry the operation.
                        break;
                    case BUFFER_UNDERFLOW:
                        int netSize = sslEngine.getSession().getPacketBufferSize();
                        // Resize buffer if needed.
                        if (netSize > readBuffer.capacity()) {
                            ByteBuffer b1 = ByteBuffer.allocate(netSize);
                            b1.put(netReadBuffer);
                            netReadBuffer = b1;
                        }
                        // Obtain more inbound network data for src,
                        // then retry the operation.
//                        netReadBuffer.compact();
                        return;
                    // other cases: CLOSED, OK.
                }
                result = sslEngine.unwrap(netReadBuffer, readBuffer);
            }
//            logger.info(result + " " + netReadBuffer + " " + readBuffer);
            netReadBuffer.compact();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

}
