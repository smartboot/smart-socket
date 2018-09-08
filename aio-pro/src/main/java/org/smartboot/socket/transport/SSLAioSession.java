/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SSLAioSession.java
 * Date: 2017-12-19
 * Author: sandao
 */

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.extension.ssl.HandshakeCallback;
import org.smartboot.socket.extension.ssl.HandshakeModel;
import org.smartboot.socket.extension.ssl.SSLService;

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
    private static final Logger logger = LoggerFactory.getLogger(SSLAioSession.class);
    private ByteBuffer netWriteBuffer;

    private ByteBuffer netReadBuffer;
    private SSLEngine sslEngine = null;

    /**
     * 完成握手置null
     */
    private HandshakeModel handshakeModel;
    /**
     * 完成握手置null
     */
    private SSLService sslService;

    /**
     * @param channel
     * @param config
     * @param aioReadCompletionHandler
     * @param aioWriteCompletionHandler
     * @param sslService                是否服务端Session
     */
    SSLAioSession(AsynchronousSocketChannel channel, IoServerConfig<T> config, ReadCompletionHandler<T> aioReadCompletionHandler, WriteCompletionHandler<T> aioWriteCompletionHandler, SSLService sslService) {
        super(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, false);
        this.handshakeModel = sslService.createSSLEngine(channel);
        this.sslService = sslService;
    }

    @Override
    void writeToChannel() {
        checkInitialized();
        if (netWriteBuffer != null && netWriteBuffer.hasRemaining()) {
            writeToChannel0(netWriteBuffer);
            return;
        }
        super.writeToChannel();
    }


    @Override
    void initSession() {
        this.sslEngine = handshakeModel.getSslEngine();
        this.netWriteBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        this.netWriteBuffer.flip();
        this.netReadBuffer = ByteBuffer.allocate(readBuffer.capacity());
//        this.serverFlowLimit = sslEngine.getUseClientMode() ? null : false;//服务端设置流控标志
        this.handshakeModel.setHandshakeCallback(new HandshakeCallback() {
            @Override
            public void callback() {
                synchronized (SSLAioSession.this) {
                    handshakeModel = null;//释放内存
                    SSLAioSession.this.notifyAll();
                }
                sslService = null;//释放内存
                SSLAioSession.this.readSemaphore.tryAcquire();
                continueRead();
            }
        });
        sslService.doHandshake(handshakeModel);
    }

    /**
     * 校验是否已完成初始化,如果还处于Handshake阶段则阻塞当前线程
     */
    private void checkInitialized() {
        if (handshakeModel == null) {
            return;
        }
        synchronized (this) {
            if (handshakeModel == null) {
                return;
            }
            try {
                this.wait();
            } catch (InterruptedException e) {
                logger.debug(e.getMessage(), e);
            }
        }
    }

    @Override
    void readFromChannel(boolean eof) {
        checkInitialized();
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
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        logger.info("doWrap BUFFER_OVERFLOW");
//                        int appSize = netWriteBuffer.capacity() * 2 < sslEngine.getSession().getPacketBufferSize() ? netWriteBuffer.capacity() * 2 : sslEngine.getSession().getPacketBufferSize();
//                        logger.info("doWrap BUFFER_OVERFLOW:" + appSize);
//                        ByteBuffer b = ByteBuffer.allocate(appSize);
//                        netWriteBuffer.flip();
//                        b.put(netWriteBuffer);
//                        netWriteBuffer = b;
                        break;
                    case BUFFER_UNDERFLOW:
                        logger.info("doWrap BUFFER_UNDERFLOW");
                        break;
                    default:
                        logger.error("doWrap Result:" + result.getStatus());
                }
                result = sslEngine.wrap(writeBuffer, netWriteBuffer);
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
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        // Could attempt to drain the dst buffer of any already obtained
                        // data, but we'll just increase it to the size needed.
                        int appSize = readBuffer.capacity() * 2 < sslEngine.getSession().getApplicationBufferSize() ? readBuffer.capacity() * 2 : sslEngine.getSession().getApplicationBufferSize();
                        logger.info("doUnWrap BUFFER_OVERFLOW:" + appSize);
                        ByteBuffer b = ByteBuffer.allocate(appSize + readBuffer.position());
                        readBuffer.flip();
                        b.put(readBuffer);
                        readBuffer = b;
                        // retry the operation.
                        break;
                    case BUFFER_UNDERFLOW:

//                        int netSize = readBuffer.capacity() * 2 < sslEngine.getSession().getPacketBufferSize() ? readBuffer.capacity() * 2 : sslEngine.getSession().getPacketBufferSize();
//                        int netSize = sslEngine.getSession().getPacketBufferSize();

                        // Resize buffer if needed.
                        if (netReadBuffer.limit() == netReadBuffer.capacity()) {
                            int netSize = netReadBuffer.capacity() * 2 < sslEngine.getSession().getPacketBufferSize() ? netReadBuffer.capacity() * 2 : sslEngine.getSession().getPacketBufferSize();
                            logger.debug("BUFFER_UNDERFLOW:" + netSize);
                            ByteBuffer b1 = ByteBuffer.allocate(netSize);
                            b1.put(netReadBuffer);
                            netReadBuffer = b1;
                        } else {
                            if (netReadBuffer.position() > 0) {
                                netReadBuffer.compact();
                            } else {
                                netReadBuffer.position(netReadBuffer.limit());
                                netReadBuffer.limit(netReadBuffer.capacity());
                            }
                            logger.debug("BUFFER_UNDERFLOW,continue read:" + netReadBuffer);
                        }
                        // Obtain more inbound network data for src,
                        // then retry the operation.
//                        netReadBuffer.compact();
                        return;
                    default:
                        logger.error("doUnWrap Result:" + result.getStatus());
                        // other cases: CLOSED, OK.
                }
                result = sslEngine.unwrap(netReadBuffer, readBuffer);
            }
            netReadBuffer.compact();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close(boolean immediate) {
        super.close(immediate);
        if (status == SESSION_STATUS_CLOSED) {
            sslEngine.closeOutbound();
        }
    }
}
