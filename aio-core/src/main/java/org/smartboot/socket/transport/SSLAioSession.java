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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.KeyStore;
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
    private boolean handshakeFinished = false;
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
        if (handshakeFinished) {
            super.writeToChannel();
            return;
        }
        writesemaphore.release();
        doHandshake(channel);
    }

    @Override
    void readFromChannel(boolean eof) {
        if (handshakeFinished) {
            doUnWrap();
            super.readFromChannel(eof);
            return;
        }
        if (eof) {
            logger.info("eof");
            return;
        }
        if (readsemaphore != null) {
            readsemaphore.release();
        }
        doHandshake(channel);
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

    private synchronized void doHandshake(final AsynchronousSocketChannel socketChannel) {
        SSLEngineResult result = null;
        try {
            initSSL();
            SSLEngineResult.HandshakeStatus handshakeStatus = null;

            while (!handshakeFinished) {
                handshakeStatus = engine.getHandshakeStatus();
                switch (handshakeStatus) {
                    case NEED_UNWRAP:

                        if (!readsemaphore.tryAcquire()) {
                            logger.error("------");
                            return;
                        }
                        netReadBuffer.flip();
                        if (!netReadBuffer.hasRemaining()) {
//                            logger.warn("need unwrap,but netReadBuffer has no remaining:" + netReadBuffer);
                            netReadBuffer.clear();
                            readFromChannel0(netReadBuffer);
                            return;
                        }
                        result = engine.unwrap(netReadBuffer, appReadBuffer);//调用SSLEngine进行unwrap操作
                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                            handshakeFinished = true;
                            System.out.println(engine.getHandshakeStatus() + "" + engine.getHandshakeStatus() + "netReadBuffer:" + netReadBuffer);
                            netReadBuffer.clear();
                            readFromChannel0(netReadBuffer);
                        }
                        switch (result.getStatus()) {
                            case OK:
                                if (!handshakeFinished) {
                                    netReadBuffer.compact();
                                }
                                readsemaphore.release();
                                break;
                            case BUFFER_OVERFLOW:
                                logger.warn("BUFFER_OVERFLOW");
                                // Will occur when appReadBuffer's capacity is smaller than the data derived from netReadBuffer's unwrap.
                                appReadBuffer = enlargeApplicationBuffer(engine, appReadBuffer);
                                break;
                            case BUFFER_UNDERFLOW:
                                logger.warn("BUFFER_UNDERFLOW");
                                // Will occur either when no data was read from the peer or when the netReadBuffer buffer was too small to hold all peer's data.
                                appReadBuffer = handleBufferUnderflow(engine.getSession(), appReadBuffer);
                                break;
                            case CLOSED:
                                if (engine.isOutboundDone()) {
                                    return;
                                } else {
                                    engine.closeOutbound();
                                    doHandshake(channel);
                                    return;
                                }
                            default:
                                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                        }
                        break;
                    case NEED_WRAP:
                        if (!writesemaphore.tryAcquire()) {
                            return;
                        }
                        if (netWriteBuffer.hasRemaining()) {
                            return;
                        }
                        netWriteBuffer.clear();
                        result = engine.wrap(appWriteBuffer, netWriteBuffer);
                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                            handshakeFinished = true;
                            System.out.println(engine.getHandshakeStatus() + "" + engine.getHandshakeStatus());
                            netReadBuffer.clear();
                            readFromChannel0(netReadBuffer);
                        }
                        switch (result.getStatus()) {
                            case OK:
                                appWriteBuffer.clear();
                                netWriteBuffer.flip();

                                writeToChannel0(netWriteBuffer);
                                if (result.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.FINISHED) {
                                    return;
                                } else {
                                    break;
                                }
                            case BUFFER_OVERFLOW:
                                netWriteBuffer = enlargePacketBuffer(engine.getSession(), netWriteBuffer);
                                break;
                            case BUFFER_UNDERFLOW:
                                throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                            case CLOSED:
                                try {
                                    netWriteBuffer.flip();
                                    while (netWriteBuffer.hasRemaining()) {
                                        socketChannel.write(netWriteBuffer).get();
                                    }
                                    // At this point the handshake status will probably be NEED_UNWRAP so we make sure that netReadBuffer is clear to read.
                                    netReadBuffer.clear();
                                } catch (Exception e) {
                                    logger.error("Failed to send server's CLOSE message due to socket channel's failure.");
                                }
                                break;
                            default:
                                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                        }
                        break;
                    case NEED_TASK:
                        Runnable task;
                        while ((task = engine.getDelegatedTask()) != null) {
                            task.run();
                        }
                        break;
                    case FINISHED:
                        logger.info("HandshakeFinished");
                        break;
                    case NOT_HANDSHAKING:
                        logger.info("NOT_HANDSHAKING");
                        break;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
                }

            }
            logger.info("hahaha:" + handshakeStatus);
        } catch (Exception e) {
            logger.catching(e);
        }
    }

    protected ByteBuffer enlargePacketBuffer(SSLSession session, ByteBuffer buffer) {
        return enlargeBuffer(buffer, session.getPacketBufferSize());
    }

    protected ByteBuffer enlargeApplicationBuffer(SSLEngine engine, ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize());
    }

    /**
     * Compares <code>sessionProposedCapacity<code> with buffer's capacity. If buffer's capacity is smaller,
     * returns a buffer with the proposed capacity. If it's equal or larger, returns a buffer
     * with capacity twice the size of the initial one.
     *
     * @param buffer                  - the buffer to be enlarged.
     * @param sessionProposedCapacity - the minimum size of the new buffer, proposed by {@link SSLSession}.
     * @return A new buffer with a larger capacity.
     */
    protected ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
        if (sessionProposedCapacity > buffer.capacity()) {
            buffer = ByteBuffer.allocate(sessionProposedCapacity);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }
        return buffer;
    }

    protected ByteBuffer handleBufferUnderflow(SSLSession session, ByteBuffer buffer) {
        if (session.getPacketBufferSize() < buffer.limit()) {
            return buffer;
        } else {
            ByteBuffer replaceBuffer = enlargePacketBuffer(session, buffer);
            buffer.flip();
            replaceBuffer.put(buffer);
            return replaceBuffer;
        }
    }

    private void initSSL() {
        if (initSSL) {
            return;
        }
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            KeyStore ts = KeyStore.getInstance("JKS");

            char[] passphrase = "storepass".toCharArray();

            if (serverFlowLimit == null) {
                ks.load(new FileInputStream("client.jks"), passphrase);
            } else {
                ks.load(new FileInputStream("server.jks"), passphrase);
            }
            ts.load(new FileInputStream("trustedCerts.jks"), "storepass".toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "keypass".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);

            SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");

            sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            engine = sslCtx.createSSLEngine();
            SSLSession session = engine.getSession();
            engine.setUseClientMode(serverFlowLimit == null);
            if (serverFlowLimit != null) {
                engine.setNeedClientAuth(true);
                engine.setWantClientAuth(true);
            }
            appWriteBuffer = ByteBuffer.allocate(session.getApplicationBufferSize());
            netWriteBuffer = ByteBuffer.allocate(session.getPacketBufferSize());
            netWriteBuffer.flip();
            appReadBuffer = ByteBuffer.allocate(session.getApplicationBufferSize());
            netReadBuffer = ByteBuffer.allocate(session.getPacketBufferSize());
            engine.beginHandshake();
            readsemaphore = new Semaphore(1);
            writesemaphore = new Semaphore(1);
            initSSL = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
