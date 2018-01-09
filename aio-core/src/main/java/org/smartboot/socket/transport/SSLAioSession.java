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
import javax.net.ssl.SSLSession;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Semaphore;

/**
 * @author 三刀
 * @version V1.0 , 2017/12/19
 */
public class SSLAioSession<T> extends AioSession<T> {
    private static final Logger logger = LogManager.getLogger(SSLAioSession.class);
    /**
     * Will contain this peer's application data in plaintext, that will be later encrypted
     * using {@link SSLEngine#wrap(ByteBuffer, ByteBuffer)} and sent to the other peer. This buffer can typically
     * be of any size, as long as it is large enough to contain this peer's outgoing messages.
     * If this peer tries to send a message bigger than buffer's capacity a {@link BufferOverflowException}
     * will be thrown.
     */
    protected ByteBuffer appWriteBuffer;
    protected ByteBuffer netWriteBuffer;

    /**
     * Will contain the other peer's (decrypted) application data. It must be large enough to hold the application data
     * from any peer. Can be initialized with {@link SSLSession#getApplicationBufferSize()} for an estimation
     * of the other peer's application data and should be enlarged if this size is not enough.
     */
    protected ByteBuffer appReadBuffer;

    /**
     * Will contain the other peer's encrypted data. The SSL/TLS protocols specify that implementations should produce packets containing at most 16 KB of plaintext,
     * so a buffer sized to this value should normally cause no capacity problems. However, some implementations violate the specification and generate large records up to 32 KB.
     * If the {@link SSLEngine#unwrap(ByteBuffer, ByteBuffer)} detects large inbound packets, the buffer sizes returned by SSLSession will be updated dynamically, so the this peer
     * should check for overflow conditions and enlarge the buffer using the session's (updated) buffer size.
     */
    protected ByteBuffer netReadBuffer;
    private SSLEngine engine = null;
    private boolean initSSL = false;
    private Semaphore writesemaphore;
    private Semaphore readsemaphore;

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

    @Override
    void readFromChannel(boolean eof) {
        doUnWrap();
        super.readFromChannel(eof);
    }

    protected void continueRead() {
        readFromChannel0(netReadBuffer);
    }

    protected void continueWrite() {
        doWrap();
        writeToChannel0(netWriteBuffer);
    }

    private void doWrap() {
        try {
            netWriteBuffer.compact();
            SSLEngineResult result = engine.wrap(writeBuffer, netWriteBuffer);
            if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
                System.exit(-1);
            }
            netWriteBuffer.flip();
            byte[] array = new byte[netWriteBuffer.remaining()];
            netWriteBuffer.get(array);
//            logger.info(StringUtils.toHexString(array));
            netWriteBuffer.flip();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private void doUnWrap() {
        try {
            netReadBuffer.flip();
            byte[] array = new byte[netReadBuffer.remaining()];
            netReadBuffer.get(array);
//            logger.info(StringUtils.toHexString(array));
            netReadBuffer.flip();
            SSLEngineResult result = engine.unwrap(netReadBuffer, readBuffer);
            netReadBuffer.compact();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

}
