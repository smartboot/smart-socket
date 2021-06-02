/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UnsupportAsynchronousSocketChannel.java
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
public class UnsupportedAsynchronousSocketChannel extends AsynchronousSocketChannel {
    public UnsupportedAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
        super(asynchronousSocketChannel.provider());
    }

    @Override
    public AsynchronousSocketChannel bind(SocketAddress local) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getOption(SocketOption<T> name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsynchronousSocketChannel shutdownInput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsynchronousSocketChannel shutdownOutput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }
}
