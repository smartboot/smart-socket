/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AbstractAsynchronousSocketChannel.java
 * Date: 2021-06-02
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/2
 */
public class AsynchronousSocketChannelProxy extends AsynchronousSocketChannel {
    protected final AsynchronousSocketChannel asynchronousSocketChannel;

    public AsynchronousSocketChannelProxy(AsynchronousSocketChannel asynchronousSocketChannel) {
        super(asynchronousSocketChannel.provider());
        this.asynchronousSocketChannel = asynchronousSocketChannel;
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
        asynchronousSocketChannel.read(dst, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        return asynchronousSocketChannel.read(dst);
    }

    @Override
    public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        asynchronousSocketChannel.read(dsts, offset, length, timeout, unit, attachment, handler);
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        asynchronousSocketChannel.write(src, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        return asynchronousSocketChannel.write(src);
    }

    @Override
    public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        asynchronousSocketChannel.write(srcs, offset, length, timeout, unit, attachment, handler);
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
    }
}
