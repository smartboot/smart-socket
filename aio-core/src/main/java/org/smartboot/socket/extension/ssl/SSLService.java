/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SSLService.java
 * Date: 2018-01-01 20:43:55
 * Author: sandao
 */

package org.smartboot.socket.extension.ssl;

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
import java.util.concurrent.Semaphore;

/**
 * TLS/SSL服务
 *
 * @author 三刀
 * @version V1.0 , 2018/1/1
 */
public class SSLService {

    private SSLContext sslContext;

    private SSLConfig config;

    public SSLService(SSLConfig config) {
        init(config);
    }


    private void init(SSLConfig config) {
        try {
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

            engine = sslContext.createSSLEngine();
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

    public SSLEngine createSSLEngine() {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(config.isClientMode());
        if (!config.isClientMode()) {
            sslEngine.setNeedClientAuth(true);
            sslEngine.setWantClientAuth(true);
        }
        return sslEngine;
    }

    public void doHandshake(final AsynchronousSocketChannel socketChannel,HandshakeCallback handshakeCallback){
        SSLEngineResult result = null;
        try {
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
}
