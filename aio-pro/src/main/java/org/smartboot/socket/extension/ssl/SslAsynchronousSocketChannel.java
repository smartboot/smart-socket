/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsAsynchronousSocketChannel.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/16
 */
public class SslAsynchronousSocketChannel extends AsynchronousSocketChannel {
    private static final Logger logger = Logger.getLogger("ssl");
    private AsynchronousSocketChannel asynchronousSocketChannel;
    private ByteBuffer netWriteBuffer;

    private ByteBuffer netReadBuffer;

    private ByteBuffer appReadBuffer;
    private SSLEngine sslEngine = null;

    /**
     * 完成握手置null
     */
    private HandshakeModel handshakeModel;
    /**
     * 完成握手置null
     */
    private SslService sslService;

    private boolean handshake = true;

    public SslAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel, SslService sslService, int readBufferSize) {
        super(null);
        this.handshakeModel = sslService.createSSLEngine(asynchronousSocketChannel);
        this.sslService = sslService;
        this.asynchronousSocketChannel = asynchronousSocketChannel;
        this.sslEngine = handshakeModel.getSslEngine();
        this.netWriteBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        this.netWriteBuffer.flip();
        this.netReadBuffer = ByteBuffer.allocate(readBufferSize);
    }

    @Override
    public AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
        return asynchronousSocketChannel.bind(local);
    }

    @Override
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        return asynchronousSocketChannel.setOption(name, value);
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return asynchronousSocketChannel.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return asynchronousSocketChannel.supportedOptions();
    }

    @Override
    public AsynchronousSocketChannel shutdownInput() throws IOException {
        return asynchronousSocketChannel.shutdownInput();
    }

    @Override
    public AsynchronousSocketChannel shutdownOutput() throws IOException {
        return asynchronousSocketChannel.shutdownOutput();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return asynchronousSocketChannel.getRemoteAddress();
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        asynchronousSocketChannel.connect(remote, attachment, handler);
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        return asynchronousSocketChannel.connect(remote);
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (handshake) {
            handshakeModel.setHandshakeCallback(new HandshakeCallback() {
                @Override
                public void callback() {
                    handshake = false;
                    synchronized (SslAsynchronousSocketChannel.this) {
                        //释放内存
                        handshakeModel = null;
                        SslAsynchronousSocketChannel.this.notifyAll();
                    }
                    SslAsynchronousSocketChannel.this.read(dst, timeout, unit, attachment, handler);
                }
            });
            //触发握手
            sslService.doHandshake(handshakeModel);
            return;
        }
        if (appReadBuffer != null) {
            int pos = dst.position();
            if (appReadBuffer.remaining() > dst.remaining()) {
                byte[] bytes = new byte[dst.remaining()];
                appReadBuffer.get(bytes);
                dst.put(bytes);
            } else {
                dst.put(appReadBuffer);
            }
            if (!appReadBuffer.hasRemaining()) {
                appReadBuffer = null;
            }
            handler.completed(dst.position() - pos, attachment);
            return;
        }
        asynchronousSocketChannel.read(netReadBuffer, timeout, unit, attachment, new CompletionHandler<Integer, A>() {
            @Override
            public void completed(Integer result, A attachment) {
                int pos = dst.position();
                appReadBuffer = dst;
                doUnWrap();
                if (appReadBuffer == dst) {
                    appReadBuffer = null;
                } else {
                    appReadBuffer.flip();
                    dst.clear();
                    if (appReadBuffer.remaining() > dst.remaining()) {
                        byte[] bytes = new byte[dst.remaining()];
                        appReadBuffer.get(bytes);
                        dst.put(bytes);
                    } else {
                        dst.put(appReadBuffer);
                    }
                    if (!appReadBuffer.hasRemaining()) {
                        appReadBuffer = null;
                    }
                }
                handler.completed(dst.position() - pos, attachment);
            }

            @Override
            public void failed(Throwable exc, A attachment) {
                handler.failed(exc, attachment);
            }
        });
    }

    private void doUnWrap() {
        try {
            netReadBuffer.flip();
            SSLEngineResult result = sslEngine.unwrap(netReadBuffer, appReadBuffer);
            boolean closed = false;
            while (!closed && result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        // Could attempt to drain the dst buffer of any already obtained
                        // data, but we'll just increase it to the size needed.
                        int appSize = appReadBuffer.capacity() * 2 < sslEngine.getSession().getApplicationBufferSize() ? appReadBuffer.capacity() * 2 : sslEngine.getSession().getApplicationBufferSize();
                        logger.info("doUnWrap BUFFER_OVERFLOW:" + appSize + ", pos:" + appReadBuffer.position());
                        ByteBuffer b = ByteBuffer.allocate(appSize + appReadBuffer.position());
                        appReadBuffer.flip();
                        b.put(appReadBuffer);
                        appReadBuffer = b;
                        // retry the operation.
                        break;
                    case BUFFER_UNDERFLOW:
                        // Resize buffer if needed.
                        if (netReadBuffer.limit() == netReadBuffer.capacity()) {
                            int netSize = netReadBuffer.capacity() * 2 < sslEngine.getSession().getPacketBufferSize() ? netReadBuffer.capacity() * 2 : sslEngine.getSession().getPacketBufferSize();
                            logger.finest("BUFFER_UNDERFLOW:" + netSize);
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
                            logger.finest("BUFFER_UNDERFLOW,continue read:" + netReadBuffer);
                        }
                        // Obtain more inbound network data for src,
                        // then retry the operation.
                        return;
                    case CLOSED:
                        logger.finest("doUnWrap Result:" + result.getStatus());
                        closed = true;
                        break;
                    default:
                        logger.finest("doUnWrap Result:" + result.getStatus());
                        // other cases: CLOSED, OK.
                }
                result = sslEngine.unwrap(netReadBuffer, appReadBuffer);
            }
            netReadBuffer.compact();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (handshake) {
            checkInitialized();
        }
        doWrap(src);
        asynchronousSocketChannel.write(netWriteBuffer, timeout, unit, attachment, new CompletionHandler<Integer, A>() {
            @Override
            public void completed(Integer result, A attachment) {
                if (netWriteBuffer.hasRemaining()) {
                    asynchronousSocketChannel.write(netWriteBuffer, timeout, unit, attachment, this);
                } else {
                    handler.completed(result, attachment);
                }
            }

            @Override
            public void failed(Throwable exc, A attachment) {
                handler.failed(exc, attachment);
            }
        });
    }

    /**
     * 校验是否已完成初始化,如果还处于Handshake阶段则阻塞当前线程
     */
    private void checkInitialized() {
        if (!handshake) {
            return;
        }
        synchronized (this) {
            if (!handshake) {
                return;
            }
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void doWrap(ByteBuffer writeBuffer) {
        try {
            netWriteBuffer.compact();
            SSLEngineResult result = sslEngine.wrap(writeBuffer, netWriteBuffer);
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        logger.info("doWrap BUFFER_OVERFLOW");
                        break;
                    case BUFFER_UNDERFLOW:
                        logger.info("doWrap BUFFER_UNDERFLOW");
                        break;
                    default:
                        logger.severe("doWrap Result:" + result.getStatus());
                }
                result = sslEngine.wrap(writeBuffer, netWriteBuffer);
            }
            netWriteBuffer.flip();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return asynchronousSocketChannel.getLocalAddress();
    }

    @Override
    public boolean isOpen() {
        return asynchronousSocketChannel.isOpen();
    }

    @Override
    public void close() throws IOException {
        asynchronousSocketChannel.close();
        sslEngine.closeOutbound();
    }
}
