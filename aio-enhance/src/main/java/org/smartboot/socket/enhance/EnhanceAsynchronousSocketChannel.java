/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: EnhanceAsynchronousSocketChannel.java
 * Date: 2021-07-29
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.ReadPendingException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritePendingException;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 模拟JDK7的AIO处理方式
 *
 * @author 三刀
 * @version V1.0 , 2018/5/24
 */
final class EnhanceAsynchronousSocketChannel extends AsynchronousSocketChannel {
    /**
     * 实际的Socket通道
     */
    private final SocketChannel channel;
    /**
     * 处理当前连接IO事件的资源组
     */
    private final EnhanceAsynchronousChannelGroup group;
    /**
     * 处理 read 事件的线程资源
     */
    private final EnhanceAsynchronousChannelGroup.Worker readWorker;
    /**
     * 处理 write 事件的线程资源
     */
    private final EnhanceAsynchronousChannelGroup.Worker writeWorker;
    /**
     * 处理 connect 事件的线程资源
     */
    private final EnhanceAsynchronousChannelGroup.Worker connectWorker;
    /**
     * 用于接收 read 通道数据的缓冲区，经解码后腾出缓冲区以供下一批数据的读取
     */
    private ByteBuffer readBuffer;
    /**
     * 用于接收 read 通道数据的缓冲区集合
     */
    private ByteBufferArray scatteringReadBuffer;
    /**
     * 存放待输出数据的缓冲区
     */
    private ByteBuffer writeBuffer;
    /**
     * 存放待输出数据的缓冲区集合
     */
    private ByteBufferArray gatheringWriteBuffer;
    /**
     * read 回调事件处理器
     */
    private CompletionHandler<Number, Object> readCompletionHandler;
    /**
     * write 回调事件处理器
     */
    private CompletionHandler<Number, Object> writeCompletionHandler;
    /**
     * connect 回调事件处理器
     */
    private CompletionHandler<Void, Object> connectCompletionHandler;
    private FutureCompletionHandler<Void, Void> connectFuture;
    private FutureCompletionHandler<? extends Number, Object> readFuture;
    private FutureCompletionHandler<? extends Number, Object> writeFuture;
    /**
     * read 回调事件关联绑定的附件对象
     */
    private Object readAttachment;
    /**
     * write 回调事件关联绑定的附件对象
     */
    private Object writeAttachment;
    /**
     * connect 回调事件关联绑定的附件对象
     */
    private Object connectAttachment;
    private SelectionKey readSelectionKey;
    private SelectionKey readFutureSelectionKey;
    private SelectionKey writeSelectionKey;
    private SelectionKey writeFutureSelectionKey;
    private SelectionKey connectSelectionKey;
    /**
     * 当前是否正在执行 write 操作
     */
    private boolean writePending;
    /**
     * 当前是否正在执行 read 操作
     */
    private boolean readPending;
    /**
     * 当前是否正在执行 connect 操作
     */
    private boolean connectionPending;
    /**
     * 远程连接的地址
     */
    private SocketAddress remote;

    public EnhanceAsynchronousSocketChannel(EnhanceAsynchronousChannelGroup group, SocketChannel channel) throws IOException {
        super(group.provider());
        this.group = group;
        this.channel = channel;
        readWorker = group.getReadWorker();
        writeWorker = group.getWriteWorker();
        connectWorker = group.getConnectWorker();
        channel.configureBlocking(false);
    }

    @Override
    public void close() throws IOException {
        IOException exception = null;
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            exception = e;
        }
        if (readSelectionKey != null) {
            readSelectionKey.cancel();
            readSelectionKey = null;
        }
        if (readFutureSelectionKey != null) {
            readFutureSelectionKey.cancel();
            readFutureSelectionKey = null;
        }
        if (writeSelectionKey != null) {
            writeSelectionKey.cancel();
            writeSelectionKey = null;
        }
        if (writeFutureSelectionKey != null) {
            writeFutureSelectionKey.cancel();
            writeFutureSelectionKey = null;
        }
        if (connectSelectionKey != null) {
            connectSelectionKey.cancel();
            connectSelectionKey = null;
        }
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
        channel.bind(local);
        return this;
    }

    @Override
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        channel.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return channel.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return channel.supportedOptions();
    }

    @Override
    public AsynchronousSocketChannel shutdownInput() throws IOException {
        channel.shutdownInput();
        return this;
    }

    @Override
    public AsynchronousSocketChannel shutdownOutput() throws IOException {
        channel.shutdownOutput();
        return this;
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return channel.getRemoteAddress();
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        if (group.isTerminated()) {
            throw new ShutdownChannelGroupException();
        }
        if (channel.isConnected()) {
            throw new AlreadyConnectedException();
        }
        if (connectionPending) {
            throw new ConnectionPendingException();
        }
        connectionPending = true;
        this.connectAttachment = attachment;
        this.connectCompletionHandler = (CompletionHandler<Void, Object>) handler;
        this.remote = remote;
        doConnect();
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        FutureCompletionHandler<Void, Void> connectFuture = new FutureCompletionHandler<>();
        connect(remote, null, connectFuture);
        this.connectFuture = connectFuture;
        return connectFuture;
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        read0(dst, null, timeout, unit, attachment, handler);
    }

    private <V extends Number, A> void read0(ByteBuffer readBuffer, ByteBufferArray scattering, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler) {
        if (readPending) {
            throw new ReadPendingException();
        }
        readPending = true;
        this.readBuffer = readBuffer;
        this.scatteringReadBuffer = scattering;
        this.readAttachment = attachment;
        if (timeout > 0) {
            readFuture = new FutureCompletionHandler<>((CompletionHandler<Number, Object>) handler, readAttachment);
            readCompletionHandler = (CompletionHandler<Number, Object>) readFuture;
            group.getScheduledExecutor().schedule(readFuture, timeout, unit);
        } else {
            this.readCompletionHandler = (CompletionHandler<Number, Object>) handler;
        }
        doRead(readFuture != null);
    }

    @Override
    public Future<Integer> read(ByteBuffer readBuffer) {
        FutureCompletionHandler<Integer, Object> readFuture = new FutureCompletionHandler<>();
        this.readFuture = readFuture;
        read(readBuffer, 0, TimeUnit.MILLISECONDS, null, readFuture);
        return readFuture;
    }

    @Override
    public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        read0(null, new ByteBufferArray(dsts, offset, length), timeout, unit, attachment, handler);
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        write0(src, null, timeout, unit, attachment, handler);
    }

    private <V extends Number, A> void write0(ByteBuffer writeBuffer, ByteBufferArray gathering, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler) {
        if (writePending) {
            throw new WritePendingException();
        }

        writePending = true;
        this.writeBuffer = writeBuffer;
        this.gatheringWriteBuffer = gathering;
        this.writeAttachment = attachment;
        if (timeout > 0) {
            writeFuture = new FutureCompletionHandler<>((CompletionHandler<Number, Object>) handler, writeAttachment);
            writeCompletionHandler = (CompletionHandler<Number, Object>) writeFuture;
            group.getScheduledExecutor().schedule(writeFuture, timeout, unit);
        } else {
            this.writeCompletionHandler = (CompletionHandler<Number, Object>) handler;
        }
        doWrite();
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        FutureCompletionHandler<Integer, Object> writeFuture = new FutureCompletionHandler<>();
        this.writeFuture = writeFuture;
        write0(src, null, 0, TimeUnit.MILLISECONDS, null, writeFuture);
        return writeFuture;
    }

    @Override
    public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        write0(null, new ByteBufferArray(srcs, offset, length), timeout, unit, attachment, handler);
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return channel.getLocalAddress();
    }

    public void doConnect() {
        try {
            //此前通过Future调用,且触发了cancel
            if (connectFuture != null && connectFuture.isDone()) {
                resetConnect();
                return;
            }
            boolean connected = channel.isConnectionPending();
            if (connected || channel.connect(remote)) {
                connected = channel.finishConnect();
            }
            if (connected) {
                CompletionHandler<Void, Object> completionHandler = connectCompletionHandler;
                Object attach = connectAttachment;
                resetConnect();
                completionHandler.completed(null, attach);
            } else if (connectSelectionKey == null) {
                connectWorker.addRegister(selector -> {
                    try {
                        connectSelectionKey = channel.register(selector, SelectionKey.OP_CONNECT, EnhanceAsynchronousSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        connectCompletionHandler.failed(e, connectAttachment);
                    }
                });
            } else {
                throw new IOException("unKnow exception");
            }
        } catch (IOException e) {
            connectCompletionHandler.failed(e, connectAttachment);
        }

    }

    private void resetConnect() {
        connectionPending = false;
        connectFuture = null;
        connectAttachment = null;
        connectCompletionHandler = null;
    }

    public void doRead(boolean direct) {
        try {
            //此前通过Future调用,且触发了cancel
            if (readFuture != null && readFuture.isDone()) {
                group.removeOps(readSelectionKey, SelectionKey.OP_READ);
                resetRead();
                return;
            }
            boolean isReadWorkThread = Thread.currentThread() == readWorker.getWorkerThread();
            boolean directRead = direct || (isReadWorkThread && readWorker.invoker++ < EnhanceAsynchronousChannelGroup.MAX_INVOKER);

            long readSize = 0;
            boolean hasRemain = true;
            if (directRead) {
                if (scatteringReadBuffer != null) {
                    readSize = channel.read(scatteringReadBuffer.getBuffers(), scatteringReadBuffer.getOffset(), scatteringReadBuffer.getLength());
                    hasRemain = hasRemaining(scatteringReadBuffer);
                } else {
                    readSize = channel.read(readBuffer);
                    hasRemain = readBuffer.hasRemaining();
                }
                //The read buffer is not full, there may be no readable data
                if (hasRemain && isReadWorkThread) {
                    readWorker.invoker = EnhanceAsynchronousChannelGroup.MAX_INVOKER;
                }
            }

            //注册至异步线程
            if (readFuture != null && readSize == 0) {
                group.removeOps(readSelectionKey, SelectionKey.OP_READ);
                group.registerFuture(selector -> {
                    try {
                        readFutureSelectionKey = channel.register(selector, SelectionKey.OP_READ, EnhanceAsynchronousSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        e.printStackTrace();
                        doRead(true);
                    }
                }, SelectionKey.OP_READ);
                return;
            }

            if (readSize != 0 || !hasRemain) {
                CompletionHandler<Number, Object> completionHandler = readCompletionHandler;
                Object attach = readAttachment;
                ByteBufferArray scattering = scatteringReadBuffer;
                resetRead();
                if (scattering == null) {
                    completionHandler.completed((int) readSize, attach);
                } else {
                    completionHandler.completed(readSize, attach);
                }

                if (!readPending && readSelectionKey != null) {
                    group.removeOps(readSelectionKey, SelectionKey.OP_READ);
                }
            } else if (readSelectionKey == null) {
                readWorker.addRegister(selector -> {
                    try {
                        readSelectionKey = channel.register(selector, SelectionKey.OP_READ, EnhanceAsynchronousSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        readCompletionHandler.failed(e, readAttachment);
                    }
                });
            } else {
                group.interestOps(readWorker, readSelectionKey, SelectionKey.OP_READ);
            }

        } catch (Throwable e) {
            if (readCompletionHandler == null) {
                e.printStackTrace();
                try {
                    close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } else {
                readCompletionHandler.failed(e, readAttachment);
            }
        }
    }

    private void resetRead() {
        readPending = false;
        readFuture = null;
        readCompletionHandler = null;
        readAttachment = null;
        readBuffer = null;
        scatteringReadBuffer = null;
    }

    private int invoker;

    public void doWrite() {
        try {
            //此前通过Future调用,且触发了cancel
            if (writeFuture != null && writeFuture.isDone()) {
                resetWrite();
                return;
            }
            //read线程自身有堆栈限制
            if (readWorker.getWorkerThread() == Thread.currentThread()) {
                invoker = 0;
            } else {
                //防止无限递归导致堆栈溢出
                invoker++;
            }
            long writeSize = 0;
            boolean hasRemain = true;
            if (invoker < EnhanceAsynchronousChannelGroup.MAX_INVOKER) {
                if (gatheringWriteBuffer != null) {
                    writeSize = channel.write(gatheringWriteBuffer.getBuffers(), gatheringWriteBuffer.getOffset(), gatheringWriteBuffer.getLength());
                    hasRemain = hasRemaining(gatheringWriteBuffer);
                } else {
                    writeSize = channel.write(writeBuffer);
                    hasRemain = writeBuffer.hasRemaining();
                }
                //The write buffer has not been emptied, there may be remaining data cannot be output
                if (Thread.currentThread() == writeWorker.getWorkerThread() && hasRemain) {
                    invoker = EnhanceAsynchronousChannelGroup.MAX_INVOKER;
                }
            } else {
                invoker = 0;
            }

            //注册至异步线程
            if (writeFuture != null && writeSize == 0) {
                group.removeOps(writeSelectionKey, SelectionKey.OP_WRITE);
                group.registerFuture(selector -> {
                    try {
                        writeFutureSelectionKey = channel.register(selector, SelectionKey.OP_WRITE, EnhanceAsynchronousSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        e.printStackTrace();
                        doWrite();
                    }
                }, SelectionKey.OP_WRITE);
                return;
            }

            if (writeSize != 0 || !hasRemain) {
                CompletionHandler<Number, Object> completionHandler = writeCompletionHandler;
                Object attach = writeAttachment;
                ByteBufferArray scattering = gatheringWriteBuffer;
                resetWrite();
                if (scattering == null) {
                    completionHandler.completed((int) writeSize, attach);
                } else {
                    completionHandler.completed(writeSize, attach);
                }
            } else if (writeSelectionKey == null) {
                writeWorker.addRegister(selector -> {
                    try {
                        writeSelectionKey = channel.register(selector, SelectionKey.OP_WRITE, EnhanceAsynchronousSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        writeCompletionHandler.failed(e, writeAttachment);
                    }
                });
            } else {
                group.interestOps(writeWorker, writeSelectionKey, SelectionKey.OP_WRITE);
            }
        } catch (Throwable e) {
            if (writeCompletionHandler == null) {
                e.printStackTrace();
                try {
                    close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } else {
                writeCompletionHandler.failed(e, writeAttachment);
            }
        }
    }

    private boolean hasRemaining(ByteBufferArray scattering) {
        for (int i = 0; i < scattering.getLength(); i++) {
            if (scattering.getBuffers()[scattering.getOffset() + i].hasRemaining()) {
                return true;
            }
        }
        return false;
    }

    private void resetWrite() {
        writePending = false;
        writeFuture = null;
        writeAttachment = null;
        writeCompletionHandler = null;
        writeBuffer = null;
        gatheringWriteBuffer = null;
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }
}
