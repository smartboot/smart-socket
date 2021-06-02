/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StreamMonitorPlugin.java
 * Date: 2021-06-02
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.channels.AsynchronousSocketChannelProxy;
import org.smartboot.socket.util.StringUtils;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 传输层码流监控插件
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/2
 */
public class StreamMonitorPlugin<T> extends AbstractPlugin<T> {
    private final Consumer<byte[]> inputStreamConsumer;
    private final Consumer<byte[]> outputStreamConsumer;

    public StreamMonitorPlugin() {
        this(bytes -> {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            System.out.println("\033[34m" + simpleDateFormat.format(new Date()) + " [Input Stream]" + StringUtils.toHexString(bytes));
        }, bytes -> {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            System.err.println("\033[31m" + simpleDateFormat.format(new Date()) + " [Output Stream]" + StringUtils.toHexString(bytes));
        });

    }

    public StreamMonitorPlugin(Consumer<byte[]> inputStreamConsumer, Consumer<byte[]> outputStreamConsumer) {
        this.inputStreamConsumer = Objects.requireNonNull(inputStreamConsumer);
        this.outputStreamConsumer = Objects.requireNonNull(outputStreamConsumer);
    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new StreamMonitorAsynchronousSocketChannel(channel);
    }

    static class MonitorCompletionHandler<A> implements CompletionHandler<Integer, A> {
        CompletionHandler<Integer, A> handler;
        Consumer<byte[]> consumer;
        ByteBuffer buffer;

        public MonitorCompletionHandler(CompletionHandler<Integer, A> handler, Consumer<byte[]> consumer, ByteBuffer buffer) {
            this.handler = handler;
            this.consumer = consumer;
            this.buffer = buffer;
        }

        @Override
        public void completed(Integer result, A attachment) {
            if (result > 0) {
                byte[] bytes = new byte[result];
                buffer.position(buffer.position() - result);
                buffer.get(bytes);
                consumer.accept(bytes);
            }
            handler.completed(result, attachment);
        }

        @Override
        public void failed(Throwable exc, A attachment) {
            handler.failed(exc, attachment);
        }
    }

    class StreamMonitorAsynchronousSocketChannel extends AsynchronousSocketChannelProxy {

        public StreamMonitorAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
            super(asynchronousSocketChannel);
        }

        @Override
        public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            super.read(dst, timeout, unit, attachment, new MonitorCompletionHandler<>(handler, inputStreamConsumer, dst));
        }

        @Override
        public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            super.write(src, timeout, unit, attachment, new MonitorCompletionHandler<>(handler, outputStreamConsumer, src));
        }
    }
}
