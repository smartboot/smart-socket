/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslService.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * TLS/SSL服务
 *
 * @author 三刀
 * @version V1.0 , 2018/1/1
 */
public class SslService {

    private static final Logger logger = LoggerFactory.getLogger(SslService.class);

    private SSLContext sslContext;

    private SslConfig config;

    private HandshakeCompletion handshakeCompletion = new HandshakeCompletion(this);

    public SslService(SslConfig config) {
        init(config);
    }

    private void init(SslConfig config) {
        try {
            this.config = config;
            KeyManager[] keyManagers = null;
            if (config.getKeyFile() != null) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new FileInputStream(config.getKeyFile()), config.getKeystorePassword().toCharArray());
                kmf.init(ks, config.getKeyPassword().toCharArray());
                keyManagers = kmf.getKeyManagers();
            }

            TrustManager[] trustManagers;
            if (config.getTrustFile() != null) {
                KeyStore ts = KeyStore.getInstance("JKS");
                ts.load(new FileInputStream(config.getTrustFile()), config.getTrustPassword().toCharArray());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ts);
                trustManagers = tmf.getTrustManagers();
            } else {
                trustManagers = new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }};
            }
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());

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
                switch (config.getClientAuth()) {
                    case OPTIONAL:
                        sslEngine.setWantClientAuth(true);
                        break;
                    case REQUIRE:
                        sslEngine.setNeedClientAuth(true);
                        break;
                    case NONE:
                        break;
                    default:
                        throw new Error("Unknown auth " + config.getClientAuth());
                }
            }
            handshakeModel.setSslEngine(sslEngine);
            handshakeModel.setAppWriteBuffer(ByteBuffer.allocate(0));
            handshakeModel.setNetWriteBuffer(ByteBuffer.allocate(session.getPacketBufferSize()));
            handshakeModel.getNetWriteBuffer().flip();
            handshakeModel.setAppReadBuffer(ByteBuffer.allocate(1));
            handshakeModel.setNetReadBuffer(ByteBuffer.allocate(1));
            sslEngine.beginHandshake();


            handshakeModel.setSocketChannel(socketChannel);
            return handshakeModel;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 纯异步实现的SSL握手,
     * 在执行doHandshake期间必须保证当前通道无数据读写正在执行。
     * 若触发了数据读写，也应立马终止doHandshake方法
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

            //握手阶段网络断链
            if (handshakeModel.isEof()) {
                logger.warn("the ssl handshake is terminated");
                handshakeModel.setFinished(true);
                return;
            }
            while (!handshakeModel.isFinished()) {
                handshakeStatus = engine.getHandshakeStatus();
                if (logger.isDebugEnabled()) {
                    logger.info("握手状态:" + handshakeStatus);
                }
                switch (handshakeStatus) {
                    case NEED_UNWRAP:
                        //解码
                        netReadBuffer.flip();
                        if (netReadBuffer.hasRemaining()) {
                            result = engine.unwrap(netReadBuffer, appReadBuffer);
                            netReadBuffer.compact();
                        } else {
                            netReadBuffer.clear();
                            handshakeModel.getSocketChannel().read(netReadBuffer, handshakeModel, handshakeCompletion);
                            return;
                        }

                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                            handshakeModel.setFinished(true);
                            netReadBuffer.clear();
                        }
                        switch (result.getStatus()) {
                            case OK:
                                break;
                            case BUFFER_OVERFLOW:
                                // Will occur when appReadBuffer's capacity is smaller than the data derived from netReadBuffer's unwrap.
                                appReadBuffer = enlargeApplicationBuffer(engine, appReadBuffer);
                                handshakeModel.setAppReadBuffer(appReadBuffer);
                                break;
                            //两种情况会触发BUFFER_UNDERFLOW,1:读到的数据不够,2:netReadBuffer空间太小
                            case BUFFER_UNDERFLOW:
                                // Will occur either when no data was read from the peer or when the netReadBuffer buffer was too small to hold all peer's data.
                                netReadBuffer = handleBufferUnderflow(engine.getSession(), netReadBuffer);
                                handshakeModel.setNetReadBuffer(netReadBuffer);
                                handshakeModel.getSocketChannel().read(netReadBuffer, handshakeModel, handshakeCompletion);
                                return;
                            default:
                                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                        }
                        break;
                    case NEED_WRAP:
                        if (netWriteBuffer.hasRemaining()) {
                            logger.warn("数据未输出完毕...");
                            handshakeModel.getSocketChannel().write(netWriteBuffer, handshakeModel, handshakeCompletion);
                            return;
                        }
                        netWriteBuffer.clear();
                        result = engine.wrap(appWriteBuffer, netWriteBuffer);
                        switch (result.getStatus()) {
                            case OK:
                                appWriteBuffer.clear();
                                netWriteBuffer.flip();
                                if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                                    handshakeModel.setFinished(true);
                                }
                                handshakeModel.getSocketChannel().write(netWriteBuffer, handshakeModel, handshakeCompletion);
                                return;
                            case BUFFER_OVERFLOW:
                                logger.warn("NEED_WRAP BUFFER_OVERFLOW");
                                netWriteBuffer = enlargePacketBuffer(engine.getSession(), netWriteBuffer);
                                if (netWriteBuffer.position() > 0) {
                                    netWriteBuffer.compact();
                                } else {
                                    netWriteBuffer.position(netWriteBuffer.limit());
                                    netWriteBuffer.limit(netWriteBuffer.capacity());
                                }
                                handshakeModel.setNetWriteBuffer(netWriteBuffer);
                                break;
                            case BUFFER_UNDERFLOW:
                                throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                            case CLOSED:
                                try {
                                    netWriteBuffer.flip();
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
            }
            logger.debug("握手完毕");
            handshakeModel.getHandshakeCallback().callback();

        } catch (Exception e) {
            try {
                handshakeModel.getSslEngine().closeInbound();
            } catch (SSLException e1) {
                e1.printStackTrace();
            }
            handshakeModel.getSslEngine().closeOutbound();
            try {
                handshakeModel.getSocketChannel().close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            logger.error("", e);
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
