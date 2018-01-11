/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SSLService.java
 * Date: 2018-01-01 20:43:55
 * Author: sandao
 */

package org.smartboot.socket.extension.ssl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.transport.SSLAioSession;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * TLS/SSL服务
 *
 * @author 三刀
 * @version V1.0 , 2018/1/1
 */
public class SSLService {

    private static final Logger logger = LogManager.getLogger(SSLAioSession.class);

    private SSLContext sslContext;

    private SSLConfig config;

    private HandshakeCompletion handshakeCompletion = new HandshakeCompletion(this);

    public SSLService(SSLConfig config) {
        init(config);
    }


    private void init(SSLConfig config) {
        try {
            this.config = config;
            KeyStore ks = KeyStore.getInstance("JKS");
            KeyStore ts = KeyStore.getInstance("JKS");

            ks.load(new FileInputStream(config.getKeyFile()), config.getKeystorePassword().toCharArray());
            ts.load(new FileInputStream(config.getTrustFile()), config.getTrustPassword().toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, config.getKeyPassword().toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);

            sslContext = SSLContext.getInstance("TLSv1.2");

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HandshakeModel createSSLEngine(AsynchronousSocketChannel socketChannel) {
        try {
            HandshakeModel handshakeModel = new HandshakeModel();
            SSLEngine sslEngine = sslContext.createSSLEngine();
            SSLSession session = sslEngine.getSession();
            sslEngine.setUseClientMode(config.isClientMode());
            if (!config.isClientMode()) {
                sslEngine.setNeedClientAuth(true);
                sslEngine.setWantClientAuth(true);
            }
            handshakeModel.setSslEngine(sslEngine);
            handshakeModel.setAppWriteBuffer(ByteBuffer.allocate(0));
            handshakeModel.setNetWriteBuffer(ByteBuffer.allocate(session.getPacketBufferSize()));
            handshakeModel.getNetWriteBuffer().flip();
            handshakeModel.setAppReadBuffer(ByteBuffer.allocate(1));
            handshakeModel.setNetReadBuffer(ByteBuffer.allocate(session.getPacketBufferSize()));
            sslEngine.beginHandshake();


            handshakeModel.setSocketChannel(socketChannel);
            return handshakeModel;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 纯异步实现的SSL握手
     *
     * @param handshakeModel
     */
    public void doHandshake(HandshakeModel handshakeModel) {
        SSLEngineResult result = null;
        try {
            SSLEngineResult.HandshakeStatus handshakeStatus = null;
            ByteBuffer netReadBuffer = handshakeModel.getNetReadBuffer();
            ByteBuffer appReadBuffer = handshakeModel.getAppReadBuffer();
            ByteBuffer netWriteBuffer = handshakeModel.getNetWriteBuffer();
            ByteBuffer appWriteBuffer = handshakeModel.getAppWriteBuffer();
            SSLEngine engine = handshakeModel.getSslEngine();
            while (!handshakeModel.isFinished()) {
                handshakeStatus = engine.getHandshakeStatus();
                logger.info("握手状态:" + handshakeStatus);
                switch (handshakeStatus) {
                    case NEED_UNWRAP://解码
                        netReadBuffer.flip();
                        if (netReadBuffer.hasRemaining()) {
                            result = engine.unwrap(netReadBuffer, appReadBuffer);//调用SSLEngine进行unwrap操作
                            netReadBuffer.compact();
                        } else {
                            netReadBuffer.clear();
                            handshakeModel.getSocketChannel().read(netReadBuffer, handshakeModel, handshakeCompletion);
                            return;
                        }

                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                            handshakeModel.setFinished(true);
                            logger.info("握手结束:" + engine.getHandshakeStatus() + "" + engine.getHandshakeStatus() + "netReadBuffer:" + netReadBuffer);
                            netReadBuffer.clear();
//                            readFromChannel0(netReadBuffer);
                        }
                        switch (result.getStatus()) {
                            case OK:

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
                                    doHandshake(handshakeModel);
                                    return;
                                }
                            default:
                                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                        }
                        break;
                    case NEED_WRAP:
//                        if (!writesemaphore.tryAcquire()) {
//                            return;
//                        }
                        if (netWriteBuffer.hasRemaining()) {
                            logger.error("数据未输出完毕...");
                            return;
                        }
                        netWriteBuffer.clear();
                        result = engine.wrap(appWriteBuffer, netWriteBuffer);
                        logger.info("观察：" + result.getHandshakeStatus() + " " + result.getHandshakeStatus());
                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                            System.out.println("a");
                        }
                        switch (result.getStatus()) {
                            case OK:
                                appWriteBuffer.clear();
                                netWriteBuffer.flip();
                                if (result.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.FINISHED) {
                                    handshakeModel.getSocketChannel().write(netWriteBuffer, handshakeModel, handshakeCompletion);
                                    return;
                                }
                                break;
                            case BUFFER_OVERFLOW:
                                netWriteBuffer = enlargePacketBuffer(engine.getSession(), netWriteBuffer);
                                break;
                            case BUFFER_UNDERFLOW:
                                throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                            case CLOSED:
                                try {
                                    netWriteBuffer.flip();
//                                    while (netWriteBuffer.hasRemaining()) {
//                                        socketChannel.write(netWriteBuffer).get();
//                                    }
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
                        System.exit(-1);
                        break;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
                }
                logger.info("握手结果：" + result.getHandshakeStatus());
                if (result != null && result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                    logger.info("握手完毕");
                    handshakeModel.setFinished(true);
                    if (netWriteBuffer.hasRemaining()) {
                        handshakeModel.getSocketChannel().write(netWriteBuffer, handshakeModel, handshakeCompletion);
                        return;
                    }

                }
            }
            logger.info("hahaha:" + handshakeStatus + ", write:" + handshakeModel.getNetWriteBuffer());
            handshakeModel.getHandshakeCallback().callback();
            logger.info("bbb:" + handshakeStatus + ", write:" + handshakeModel.getNetWriteBuffer());

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

}
