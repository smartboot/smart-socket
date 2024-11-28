/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslService.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;


import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.function.Consumer;

/**
 * TLS/SSL服务
 * keytool -genkey -validity 36000 -alias www.smartboot.org -keyalg RSA -keystore server.keystore
 *
 * @author 三刀
 * @version V1.0 , 2018/1/1
 */
public final class SslService {
    private boolean debug;
    private final SSLContext sslContext;

    private final Consumer<SSLEngine> consumer;
    private final CompletionHandler<Integer, HandshakeModel> handshakeCompletionHandler = new CompletionHandler<Integer, HandshakeModel>() {
        @Override
        public void completed(Integer result, HandshakeModel attachment) {
            if (result == -1) {
                failed(new IOException("eof"), attachment);
                return;
            }
            if (result != EnhanceAsynchronousChannelProvider.READ_MONITOR_SIGNAL) {
                synchronized (attachment) {
                    doHandshake(attachment);
                }
            }
        }

        @Override
        public void failed(Throwable exc, HandshakeModel attachment) {
            attachment.setException(exc);
            attachment.getHandshakeCallback().callback();
        }
    };

    public SslService(SSLContext sslContext, Consumer<SSLEngine> consumer) {
        this.sslContext = sslContext;
        this.consumer = consumer;
    }

    HandshakeModel createSSLEngine(AsynchronousSocketChannel socketChannel, BufferPage bufferPage) {
        try {
            HandshakeModel handshakeModel = new HandshakeModel();
            SSLEngine sslEngine = sslContext.createSSLEngine();
            SSLSession session = sslEngine.getSession();

            //更新SSLEngine配置
            consumer.accept(sslEngine);

            handshakeModel.setSslEngine(sslEngine);
            handshakeModel.setAppWriteBuffer(bufferPage.allocate(session.getApplicationBufferSize()));
            handshakeModel.setNetWriteBuffer(bufferPage.allocate(session.getPacketBufferSize()));
            handshakeModel.getNetWriteBuffer().buffer().flip();
            handshakeModel.setAppReadBuffer(bufferPage.allocate(session.getApplicationBufferSize()));
            handshakeModel.setNetReadBuffer(bufferPage.allocate(session.getPacketBufferSize()));
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
            ByteBuffer netReadBuffer = handshakeModel.getNetReadBuffer().buffer();
            ByteBuffer appReadBuffer = handshakeModel.getAppReadBuffer().buffer();
            ByteBuffer netWriteBuffer = handshakeModel.getNetWriteBuffer().buffer();
            ByteBuffer appWriteBuffer = handshakeModel.getAppWriteBuffer().buffer();
            SSLEngine engine = handshakeModel.getSslEngine();

            //握手阶段网络断链
            if (handshakeModel.getException() != null) {
                if (debug) {
                    System.out.println("the ssl handshake is terminated");
                }
                handshakeModel.getHandshakeCallback().callback();
                return;
            }
            while (!handshakeModel.isFinished()) {
                handshakeStatus = engine.getHandshakeStatus();
                if (debug) {
                    System.out.println("握手状态:" + handshakeStatus);
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
                            handshakeModel.getSocketChannel().read(netReadBuffer, handshakeModel, handshakeCompletionHandler);
                            return;
                        }

                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                            handshakeModel.setFinished(true);
                        }
                        switch (result.getStatus()) {
                            case OK:
                                break;
                            case BUFFER_OVERFLOW:
                                System.err.println("doHandshake BUFFER_OVERFLOW");
                                break;
                            //两种情况会触发BUFFER_UNDERFLOW,1:读到的数据不够,2:netReadBuffer空间太小
                            case BUFFER_UNDERFLOW:
                                if (netReadBuffer.position() > 0) {
                                    handshakeModel.getSocketChannel().read(netReadBuffer, handshakeModel, handshakeCompletionHandler);
                                } else {
                                    System.out.println("doHandshake BUFFER_UNDERFLOW");
                                }
                                return;
                            default:
                                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                        }
                        break;
                    case NEED_WRAP:
                        if (netWriteBuffer.hasRemaining()) {
                            if (debug) {
                                System.out.println("数据未输出完毕...");
                            }
                            handshakeModel.getSocketChannel().write(netWriteBuffer, handshakeModel, handshakeCompletionHandler);
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
                                handshakeModel.getSocketChannel().write(netWriteBuffer, handshakeModel, handshakeCompletionHandler);
                                return;
                            case BUFFER_OVERFLOW:
                                if (debug) {
                                    System.out.println("NEED_WRAP BUFFER_OVERFLOW");
                                }
                                break;
                            case BUFFER_UNDERFLOW:
                                throw new SSLException("Buffer underflow occurred after a wrap. I don't think we should ever get here.");
                            case CLOSED:
                                if (debug) {
                                    System.out.println("closed");
                                }
                                try {
                                    netWriteBuffer.flip();
                                    netReadBuffer.clear();
                                } catch (Exception e) {
                                    if (debug) {
                                        System.out.println("Failed to send server's CLOSE message due to socket channel's failure.");
                                    }
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
                        if (debug) {
                            System.out.println("HandshakeFinished");
                        }
                        break;
                    case NOT_HANDSHAKING:
                        if (debug) {
                            System.out.println("NOT_HANDSHAKING");
                        }
                        break;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
                }
            }
            if (debug) {
                System.out.println("握手完毕");
            }
            handshakeModel.getHandshakeCallback().callback();

        } catch (Exception e) {
            if (debug) {
                System.out.println("ignore doHandshake exception:" + e.getMessage());
            }
            handshakeCompletionHandler.failed(e, handshakeModel);
        }
    }

    public void debug(boolean debug) {
        this.debug = debug;
    }

    boolean isDebug() {
        return debug;
    }
}
