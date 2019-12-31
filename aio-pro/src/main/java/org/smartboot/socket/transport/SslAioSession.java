/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslAioSession.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.extension.ssl.HandshakeCallback;
import org.smartboot.socket.extension.ssl.HandshakeModel;
import org.smartboot.socket.extension.ssl.SslService;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2017/12/19
 */
class SslAioSession<T> extends TcpAioSession<T> {
    private static final Logger logger = LoggerFactory.getLogger(SslAioSession.class);
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
    private SslService sslService;

    /**
     * @param channel
     * @param config
     * @param aioReadCompletionHandler
     * @param aioWriteCompletionHandler
     * @param sslService                是否服务端Session
     */
    SslAioSession(AsynchronousSocketChannel channel, IoServerConfig<T> config, ReadCompletionHandler<T> aioReadCompletionHandler, WriteCompletionHandler<T> aioWriteCompletionHandler, SslService sslService, BufferPage bufferPage) {
        super(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, bufferPage);
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
        this.netReadBuffer = ByteBuffer.allocate(readBuffer.buffer().capacity());
//        this.serverFlowLimit = sslEngine.getUseClientMode() ? null : false;//服务端设置流控标志
        this.handshakeModel.setHandshakeCallback(new HandshakeCallback() {
            @Override
            public void callback() {
                synchronized (SslAioSession.this) {
                    //释放内存
                    handshakeModel = null;
                    SslAioSession.this.notifyAll();
                }
                sslService = null;
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
    protected void continueWrite(VirtualBuffer writeBuffer) {
        doWrap(writeBuffer);
        writeToChannel0(netWriteBuffer);
    }

    private void doWrap(VirtualBuffer writeBuffer) {
        try {
            netWriteBuffer.compact();
            SSLEngineResult result = sslEngine.wrap(writeBuffer.buffer(), netWriteBuffer);
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        logger.info("doWrap BUFFER_OVERFLOW");
                        break;
                    case BUFFER_UNDERFLOW:
                        logger.info("doWrap BUFFER_UNDERFLOW");
                        break;
                    default:
                        logger.error("doWrap Result:" + result.getStatus());
                }
                result = sslEngine.wrap(writeBuffer.buffer(), netWriteBuffer);
            }
            netWriteBuffer.flip();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private void doUnWrap() {
        try {
            netReadBuffer.flip();
            ByteBuffer readBuffer = super.readBuffer.buffer();
            SSLEngineResult result = sslEngine.unwrap(netReadBuffer, readBuffer);
            boolean closed = false;
            while (!closed && result.getStatus() != SSLEngineResult.Status.OK) {
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
                        return;
                    case CLOSED:
                        logger.debug("doUnWrap Result:" + result.getStatus());
                        closed = true;
                        break;
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
