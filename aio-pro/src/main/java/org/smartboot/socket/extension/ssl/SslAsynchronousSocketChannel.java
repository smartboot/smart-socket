/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsAsynchronousSocketChannel.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;

import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.channels.AsynchronousSocketChannelProxy;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import org.smartboot.socket.enhance.FutureCompletionHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/16
 */
public class SslAsynchronousSocketChannel extends AsynchronousSocketChannelProxy {
    private final VirtualBuffer netWriteBuffer;
    private final VirtualBuffer netReadBuffer;
    private final VirtualBuffer appReadBuffer;
    private SSLEngine sslEngine = null;
    /**
     * 完成握手置null
     */
    private final HandshakeModel handshakeModel;
    /**
     * 完成握手置null
     */
    private final SslService sslService;

    private boolean handshake = true;
    private CountDownLatch handshakeLatch = new CountDownLatch(1);
    /**
     * 自适应的输出长度
     */
    private int adaptiveWriteSize = -1;
    private boolean closed = false;

    public SslAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel, SslService sslService, BufferPagePool bufferPage) {
        super(asynchronousSocketChannel);
        this.handshakeModel = sslService.createSSLEngine(asynchronousSocketChannel, bufferPage);
        this.sslService = sslService;
        this.sslEngine = handshakeModel.getSslEngine();
        this.netWriteBuffer = handshakeModel.getNetWriteBuffer();
        this.netReadBuffer = handshakeModel.getNetReadBuffer();
        this.appReadBuffer = handshakeModel.getAppReadBuffer();
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        //处于握手阶段
        if (handshake) {
            handshakeModel.setHandshakeCompletionHandler(new HandshakeCompletionHandler() {
                /**
                 * 握手结束后释放资源
                 */
                private void release(HandshakeModel model) {
                    model.getAppWriteBuffer().clean();
                    model.getNetReadBuffer().buffer().flip();
                    model.getNetWriteBuffer().buffer().clear();
                    model.getAppReadBuffer().buffer().clear().flip();
                }

                @Override
                public void completed(HandshakeModel model) {
                    if (!handshake) {
                        return;
                    }
                    release(model);
                    handshake = false;
                    handshakeLatch.countDown();
                    read(dst, timeout, unit, attachment, handler);
                }

                @Override
                public void failed(Throwable exc, HandshakeModel model) {
                    if (!handshake) {
                        return;
                    }
                    release(model);
                    handshake = false;
                    handshakeLatch.countDown();
                    handler.failed(exc, attachment);
                }
            });
            //触发握手
            sslService.doHandshake(handshakeModel);
            return;
        }

        ByteBuffer appBuffer = appReadBuffer.buffer();
        //appBuffer还有残留数据，先腾空
        if (appBuffer.hasRemaining()) {
            handleAppBuffer(dst, attachment, handler, appBuffer);
            return;
        }
        //netBuffer还有残留，尝试解码
        ByteBuffer netBuffer = netReadBuffer.buffer();
        if (netBuffer.hasRemaining()) {
            appBuffer.compact();
            try {
                doUnWrap(netBuffer, appBuffer);
            } catch (SSLException e) {
                handler.failed(e, attachment);
            }
            appBuffer.flip();
        }
        //appBuffer还有残留数据，先腾空
        if (appBuffer.hasRemaining()) {
            handleAppBuffer(dst, attachment, handler, appBuffer);
            return;
        }

        netBuffer.compact();
        asynchronousSocketChannel.read(netBuffer, timeout, unit, attachment, new CompletionHandler<Integer, A>() {

            @Override
            public void completed(Integer result, A attachment) {
                if (result == EnhanceAsynchronousChannelProvider.READ_MONITOR_SIGNAL) {
                    return;
                } else if (result == EnhanceAsynchronousChannelProvider.READABLE_SIGNAL) {
                    asynchronousSocketChannel.read(netBuffer, timeout, unit, attachment, this);
                    return;
                } else if (result == -1) {
                    handler.completed(result, attachment);
                    return;
                }

                // 密文解包
                ByteBuffer appBuffer = appReadBuffer.buffer();
                if (appBuffer.hasRemaining()) {
                    failed(new IOException("解包算法异常..."), attachment);
                    return;
                }
                appBuffer.clear();
                ByteBuffer netBuffer = netReadBuffer.buffer();
                netBuffer.flip();
                try {
                    doUnWrap(netBuffer, appBuffer);
                } catch (SSLException e) {
                    handler.failed(e, attachment);
                }
                appBuffer.flip();

                ////存在doUnWrap为ok，但appBuffer无数据的情况
                if (appBuffer.hasRemaining()) {
                    handleAppBuffer(dst, attachment, handler, appBuffer);
                } else {
                    netBuffer.compact();
                    asynchronousSocketChannel.read(netBuffer, timeout, unit, attachment, this);
                }

            }

            @Override
            public void failed(Throwable exc, A attachment) {
                handler.failed(exc, attachment);
            }
        });

    }

    private <A> void handleAppBuffer(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler, ByteBuffer appBuffer) {
        int pos = dst.position();
        if (appBuffer.remaining() > dst.remaining()) {
            int limit = appBuffer.limit();
            appBuffer.limit(appBuffer.position() + dst.remaining());
            dst.put(appBuffer);
            appBuffer.limit(limit);
        } else {
            dst.put(appBuffer);
        }
        handler.completed(dst.position() - pos, attachment);
    }


    private void doUnWrap(ByteBuffer netBuffer, ByteBuffer appBuffer) throws SSLException {
        SSLEngineResult result;
        do {
            result = sslEngine.unwrap(netBuffer, appBuffer);
        } while (result.getStatus() == SSLEngineResult.Status.OK && netBuffer.hasRemaining() && appBuffer.hasRemaining());

//        switch (result.getStatus()) {
//            case BUFFER_OVERFLOW:
//                if (sslService.isDebug()) {
//                    System.out.println("BUFFER_OVERFLOW error,net:" + netBuffer + " app:" + appBuffer);
//                }
//                break;
//            case BUFFER_UNDERFLOW:
//                if (netBuffer.limit() == netBuffer.capacity() && !netBuffer.hasRemaining()) {
//                    if (sslService.isDebug()) {
//                        System.err.println("BUFFER_UNDERFLOW error");
//                    }
//                } else {
//                    if (sslService.isDebug()) {
//                        System.out.println("BUFFER_UNDERFLOW,continue read:" + netBuffer);
//                    }
//                }
//                break;
//            case CLOSED:
//                if (sslService.isDebug()) {
//                    System.out.println("doUnWrap Result:" + result.getStatus());
//                }
//                break;
//            default:
//                if (sslService.isDebug()) {
//                    System.out.println("doUnWrap Result:" + result.getStatus());
//                }
//        }
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        FutureCompletionHandler<Integer, Object> readFuture = new FutureCompletionHandler<>();
        read(dst, 0, TimeUnit.MILLISECONDS, null, readFuture);
        return readFuture;
    }

    @Override
    public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (handshake) {
            try {
                handshakeLatch.await();
            } catch (InterruptedException e) {
                handler.failed(e, attachment);
                return;
            }
        }
        int pos = src.position();
        try {
            doWrap(src);
        } catch (SSLException e) {
            handler.failed(e, attachment);
            return;
        }
        if (src.position() - pos == 0) {
            if (sslEngine.getSession().isValid()) {
                System.err.println("write error:" + src + " netWrite:" + netWriteBuffer.buffer());
            } else {
                handler.failed(new IOException("connections is closed"), attachment);
                return;
            }
        }
        asynchronousSocketChannel.write(netWriteBuffer.buffer(), timeout, unit, attachment, new CompletionHandler<Integer, A>() {
            @Override
            public void completed(Integer result, A attachment) {
                if (result == -1) {
                    System.err.println("aaaaaaaaaaa");
                }
                if (netWriteBuffer.buffer().hasRemaining()) {
                    asynchronousSocketChannel.write(netWriteBuffer.buffer(), timeout, unit, attachment, this);
                } else {
                    netWriteBuffer.buffer().clear();
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

    private void doWrap(ByteBuffer writeBuffer) throws SSLException {
        ByteBuffer netBuffer = netWriteBuffer.buffer();
        if (netBuffer.position() > 0) {
            throw new IllegalStateException("netBuffer.position() > 0");
        }
        int limit = writeBuffer.limit();
        if (adaptiveWriteSize > 0 && writeBuffer.remaining() > adaptiveWriteSize) {
            writeBuffer.limit(writeBuffer.position() + adaptiveWriteSize);
        }
        SSLEngineResult result = sslEngine.wrap(writeBuffer, netBuffer);
        while (result.getStatus() != SSLEngineResult.Status.OK) {
            switch (result.getStatus()) {
                case BUFFER_OVERFLOW:
                    netBuffer.clear();
                    writeBuffer.limit(writeBuffer.position() + ((writeBuffer.limit() - writeBuffer.position() >> 1)));
                    adaptiveWriteSize = writeBuffer.remaining();
                    break;
                case BUFFER_UNDERFLOW:
                    if (sslService.isDebug()) {
                        System.err.println("doWrap BUFFER_UNDERFLOW");
                    }
                    break;
                case CLOSED:
                    throw new SSLException("SSLEngine has " + result.getStatus());
                default:
                    if (sslService.isDebug()) {
                        System.out.println("doWrap Result:" + result.getStatus());
                    }
            }
            result = sslEngine.wrap(writeBuffer, netBuffer);
        }
        writeBuffer.limit(limit);
        netBuffer.flip();

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
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        netWriteBuffer.clean();
        netReadBuffer.clean();
        appReadBuffer.clean();
        try {
            sslEngine.closeInbound();
        } catch (SSLException ignore) {
        }
        sslEngine.closeOutbound();
        asynchronousSocketChannel.close();
    }
}
