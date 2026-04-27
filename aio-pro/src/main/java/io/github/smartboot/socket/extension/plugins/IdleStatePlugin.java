/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IdleStatePlugin.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.extension.plugins;

import io.github.smartboot.socket.Plugin;
import io.github.smartboot.socket.channels.AsynchronousSocketChannelProxy;
import io.github.smartboot.socket.timer.HashedWheelTimer;
import io.github.smartboot.socket.timer.TimerTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * 空闲IO状态监听插件
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/10/22
 */
public final class IdleStatePlugin<T> implements Plugin<T> {

    private static final HashedWheelTimer timer = new HashedWheelTimer(r -> {
        Thread thread = new Thread(r, "idleStateMonitor");
        thread.setDaemon(true);
        return thread;
    });


    private final int idleTimeout;

    private final boolean writeMonitor;
    private final boolean readMonitor;

    public IdleStatePlugin(int idleTimeout) {
        this(idleTimeout, true, true);
    }

    public IdleStatePlugin(int idleTimeout, boolean readMonitor, boolean writeMonitor) {
        if (idleTimeout <= 0) {
            throw new IllegalArgumentException("invalid idleTimeout");
        }
        if (!writeMonitor && !readMonitor) {
            throw new IllegalArgumentException("readIdle and writeIdle both disable");
        }
        this.idleTimeout = idleTimeout;
        this.writeMonitor = writeMonitor;
        this.readMonitor = readMonitor;
    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new IdleMonitorChannel(channel);
    }

    class IdleMonitorChannel extends AsynchronousSocketChannelProxy {
        TimerTask task;
        long readTimestamp;
        long writeTimestamp;

        public IdleMonitorChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
            super(asynchronousSocketChannel);
            if (!IdleStatePlugin.this.readMonitor) {
                readTimestamp = Long.MAX_VALUE;
            }
            if (!IdleStatePlugin.this.writeMonitor) {
                writeTimestamp = Long.MAX_VALUE;
            }
            this.task = timer.scheduleWithFixedDelay(() -> {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - readTimestamp) > IdleStatePlugin.this.idleTimeout && (currentTime - writeTimestamp) > IdleStatePlugin.this.idleTimeout) {
                    try {
                        close();
                    } catch (IOException ignore) {
                    }
                }
            }, IdleStatePlugin.this.idleTimeout, TimeUnit.MILLISECONDS);
        }

        @Override
        public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (IdleStatePlugin.this.readMonitor) {
                readTimestamp = System.currentTimeMillis();
            }
            super.read(dst, timeout, unit, attachment, handler);
        }

        @Override
        public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (IdleStatePlugin.this.writeMonitor) {
                writeTimestamp = System.currentTimeMillis();
            }
            super.write(src, timeout, unit, attachment, handler);
        }

        @Override
        public void close() throws IOException {
            task.cancel();
            super.close();
        }
    }
}
