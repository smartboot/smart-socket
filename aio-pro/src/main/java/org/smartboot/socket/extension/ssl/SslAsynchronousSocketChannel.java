/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsAsynchronousSocketChannel.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;

import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.VirtualBuffer;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/16
 */
public class SslAsynchronousSocketChannel extends AsynchronousSocketChannel {
    private static final Logger logger = Logger.getLogger("ssl");
    private final VirtualBuffer netWriteBuffer;
    private final BufferPage bufferPage;
    private final VirtualBuffer netReadBuffer;
    private AsynchronousSocketChannel asynchronousSocketChannel;
    private VirtualBuffer appReadBuffer;
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

    public SslAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel, SslService sslService, BufferPage bufferPage) {
        super(null);
        this.bufferPage = bufferPage;
        this.handshakeModel = sslService.createSSLEngine(asynchronousSocketChannel);
        this.sslService = sslService;
        this.asynchronousSocketChannel = asynchronousSocketChannel;
        this.sslEngine = handshakeModel.getSslEngine();
        this.netWriteBuffer = bufferPage.allocate(sslEngine.getSession().getPacketBufferSize());
        this.netReadBuffer = bufferPage.allocate(sslEngine.getSession().getPacketBufferSize());
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
            if (appReadBuffer.buffer().remaining() > dst.remaining()) {
                byte[] bytes = new byte[dst.remaining()];
                appReadBuffer.buffer().get(bytes);
                dst.put(bytes);
            } else {
                dst.put(appReadBuffer.buffer());
            }
            if (!appReadBuffer.buffer().hasRemaining()) {
                appReadBuffer.clean();
                appReadBuffer = null;
            }
            handler.completed(dst.position() - pos, attachment);
            return;
        }
        asynchronousSocketChannel.read(netReadBuffer.buffer(), timeout, unit, attachment, new CompletionHandler<Integer, A>() {
            @Override
            public void completed(Integer result, A attachment) {
                int pos = dst.position();
                appReadBuffer = bufferPage.allocate(1024);
                doUnWrap();
                appReadBuffer.buffer().flip();
                if (appReadBuffer.buffer().remaining() > dst.remaining()) {
                    byte[] bytes = new byte[dst.remaining()];
                    appReadBuffer.buffer().get(bytes);
                    dst.put(bytes);
                } else if (appReadBuffer.buffer().hasRemaining()) {
                    dst.put(appReadBuffer.buffer());
                } else if (result > 0) {
                    asynchronousSocketChannel.read(netReadBuffer.buffer(), timeout, unit, attachment, this);
                    return;
                }
                if (!appReadBuffer.buffer().hasRemaining()) {
                    appReadBuffer.clean();
                    appReadBuffer = null;
                }
                if (dst.position() == pos) {
                    System.out.println("haha");
                }
                handler.completed(result != -1 ? dst.position() - pos : result, attachment);
            }

            @Override
            public void failed(Throwable exc, A attachment) {
                handler.failed(exc, attachment);
            }
        });
    }

    private void doUnWrap() {
        try {
            ByteBuffer netBuffer = netReadBuffer.buffer();
            ByteBuffer appBuffer = appReadBuffer.buffer();
            netBuffer.flip();
            SSLEngineResult result = sslEngine.unwrap(netBuffer, appBuffer);
            boolean closed = false;
            while (!closed && result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        // Could attempt to drain the dst buffer of any already obtained
                        // data, but we'll just increase it to the size needed.
                        int appSize = appBuffer.capacity() * 2 < sslEngine.getSession().getApplicationBufferSize() ? appBuffer.capacity() * 2 : sslEngine.getSession().getApplicationBufferSize();
//                        logger.info("doUnWrap BUFFER_OVERFLOW:" + appSize + ", pos:" + appBuffer.position());
                        VirtualBuffer b = bufferPage.allocate(appSize + appBuffer.position());
                        appBuffer.flip();
                        b.buffer().put(appBuffer);
                        appReadBuffer.clean();
                        appReadBuffer = b;
                        appBuffer = appReadBuffer.buffer();
                        // retry the operation.
                        break;
                    case BUFFER_UNDERFLOW:
                        // Resize buffer if needed.
                        if (netBuffer.limit() == netBuffer.capacity()) {
                            logger.severe("BUFFER_UNDERFLOW error");
//                            int netSize = netBuffer.capacity() * 2 < sslEngine.getSession().getPacketBufferSize() ? netBuffer.capacity() * 2 : sslEngine.getSession().getPacketBufferSize();
//                            logger.warning("BUFFER_UNDERFLOW:" + netSize);
//                            VirtualBuffer b1 = bufferPage.allocate(netSize);
//                            b1.buffer().put(netBuffer);
//                            netReadBuffer.clean();
//                            netReadBuffer = b1;
                        } else {
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.finest("BUFFER_UNDERFLOW,continue read:" + netBuffer);
                            }
                            if (netBuffer.position() > 0) {
                                netBuffer.compact();
                            } else {
                                netBuffer.position(netBuffer.limit());
                                netBuffer.limit(netBuffer.capacity());
                            }
                        }
                        // Obtain more inbound network data for src,
                        // then retry the operation.
                        return;
                    case CLOSED:
                        logger.warning("doUnWrap Result:" + result.getStatus());
                        closed = true;
                        break;
                    default:
                        logger.warning("doUnWrap Result:" + result.getStatus());
                        // other cases: CLOSED, OK.
                }
                result = sslEngine.unwrap(netBuffer, appBuffer);
            }
            netBuffer.compact();
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
        int pos = src.position();
        doWrap(src);
        asynchronousSocketChannel.write(netWriteBuffer.buffer(), timeout, unit, attachment, new CompletionHandler<Integer, A>() {
            @Override
            public void completed(Integer result, A attachment) {
                if (result == -1) {
                    System.err.println("aaaaaaaaaaa");
                }
                if (netWriteBuffer.buffer().hasRemaining()) {
                    asynchronousSocketChannel.write(netWriteBuffer.buffer(), timeout, unit, attachment, this);
                } else {
                    handler.completed(src.position() - pos, attachment);
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
            ByteBuffer netBuffer = netWriteBuffer.buffer();
            netBuffer.compact();
            int limit = writeBuffer.limit();
            SSLEngineResult result = sslEngine.wrap(writeBuffer, netBuffer);
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
//                        logger.info("doWrap BUFFER_OVERFLOW");
                        netBuffer.clear();
                        writeBuffer.limit(writeBuffer.position() + ((writeBuffer.limit() - writeBuffer.position() >> 1)));
                        break;
                    case BUFFER_UNDERFLOW:
                        logger.info("doWrap BUFFER_UNDERFLOW");
                        break;
                    default:
                        logger.severe("doWrap Result:" + result.getStatus());
                }
                result = sslEngine.wrap(writeBuffer, netBuffer);
            }
            writeBuffer.limit(limit);
            netBuffer.flip();
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
        netWriteBuffer.clean();
        netReadBuffer.clean();
        if (appReadBuffer != null) {
            appReadBuffer.clean();
        }
    }
}
