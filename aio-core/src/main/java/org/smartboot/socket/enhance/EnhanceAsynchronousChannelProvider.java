/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: EnhanceAsynchronousChannelProvider.java
 * Date: 2021-07-29
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2020/5/25
 */
public final class EnhanceAsynchronousChannelProvider extends AsynchronousChannelProvider {
    /**
     * 读监听信号
     */
    public static final int READ_MONITOR_SIGNAL = -2;
    /**
     * 可读信号
     */
    public static final int READABLE_SIGNAL = -3;
    /**
     * 低内存模式
     */
    private final boolean lowMemory;

    public EnhanceAsynchronousChannelProvider(boolean lowMemory) {
        this.lowMemory = lowMemory;
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory threadFactory) throws IOException {
        return new EnhanceAsynchronousChannelGroup(this, new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(nThreads),
                threadFactory), nThreads);
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int initialSize) throws IOException {
        return new EnhanceAsynchronousChannelGroup(this, executor, initialSize);
    }

    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return new EnhanceAsynchronousServerSocketChannel(checkAndGet(group), lowMemory);
    }

    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return new EnhanceAsynchronousClientChannel(checkAndGet(group), SocketChannel.open(), lowMemory);
    }

    private EnhanceAsynchronousChannelGroup checkAndGet(AsynchronousChannelGroup group) {
        if (!(group instanceof EnhanceAsynchronousChannelGroup)) {
            throw new RuntimeException("invalid class");
        }
        return (EnhanceAsynchronousChannelGroup) group;
    }
}
