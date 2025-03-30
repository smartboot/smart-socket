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
 * 增强型异步通道提供者实现类，继承自AsynchronousChannelProvider。
 * 该类主要负责创建和管理异步通道组、服务器Socket通道和客户端Socket通道。
 * 主要功能包括：
 * 1. 创建和管理异步通道组，支持多线程处理
 * 2. 提供服务器端和客户端Socket通道的创建
 * 3. 支持低内存模式运行，优化资源使用
 * 
 * 该类是smart-socket框架中异步IO实现的核心组件之一，通过NIO实现了类似JDK7 AIO的编程模型，
 * 但性能更优，资源占用更少。在低内存模式下，会采用特殊的内存管理策略以减少内存占用。
 * 
 * @author 三刀
 * @version V1.0 , 2020/5/25
 */
public final class EnhanceAsynchronousChannelProvider extends AsynchronousChannelProvider {
    /**
     * 读监听信号
     * 用于标识通道处于读监听状态，值为-2
     */
    public static final int READ_MONITOR_SIGNAL = -2;
    /**
     * 可读信号
     * 用于标识通道数据可读状态，值为-3
     */
    public static final int READABLE_SIGNAL = -3;
    /**
     * 低内存模式标志
     * 当设置为true时，系统将采用更保守的内存使用策略，
     * 适用于内存资源受限的环境
     */
    private final boolean lowMemory;

    public EnhanceAsynchronousChannelProvider(boolean lowMemory) {
        this.lowMemory = lowMemory;
    }

    /**
     * 创建一个新的异步通道组
     * 使用指定的线程数和线程工厂创建一个新的异步通道组，用于管理异步通道
     *
     * @param nThreads 线程池中的线程数量
     * @param threadFactory 创建线程的工厂类
     * @return 返回新创建的异步通道组实例
     * @throws IOException 如果创建过程中发生IO错误
     */
    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory threadFactory) throws IOException {
        return new EnhanceAsynchronousChannelGroup(this, new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(nThreads), threadFactory), nThreads);
    }

    /**
     * 使用现有的线程池创建异步通道组
     * 
     * @param executor 用于执行异步IO操作的线程池
     * @param initialSize 初始大小，用于确定内部数据结构的初始容量
     * @return 返回新创建的异步通道组实例
     * @throws IOException 如果创建过程中发生IO错误
     */
    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int initialSize) throws IOException {
        return new EnhanceAsynchronousChannelGroup(this, executor, initialSize);
    }

    /**
     * 创建一个新的异步服务器Socket通道
     * 用于服务器端接受客户端连接请求
     * 
     * @param group 关联的异步通道组，用于管理该通道的IO操作
     * @return 返回新创建的服务器Socket通道
     * @throws IOException 如果创建过程中发生IO错误
     */
    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return new EnhanceAsynchronousServerSocketChannel(checkAndGet(group), lowMemory);
    }

    /**
     * 创建一个新的异步客户端Socket通道
     * 用于客户端发起连接请求和数据传输
     * 
     * @param group 关联的异步通道组，用于管理该通道的IO操作
     * @return 返回新创建的客户端Socket通道
     * @throws IOException 如果创建过程中发生IO错误
     */
    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return new EnhanceAsynchronousSocketChannel(checkAndGet(group), SocketChannel.open(), lowMemory);
    }

    /**
     * 检查并获取增强型异步通道组实例
     * 验证传入的通道组是否为EnhanceAsynchronousChannelGroup类型
     * 
     * @param group 待检查的异步通道组
     * @return 返回转换后的增强型异步通道组
     * @throws RuntimeException 如果传入的通道组类型不正确
     */
    private EnhanceAsynchronousChannelGroup checkAndGet(AsynchronousChannelGroup group) {
        if (!(group instanceof EnhanceAsynchronousChannelGroup)) {
            throw new RuntimeException("invalid class");
        }
        return (EnhanceAsynchronousChannelGroup) group;
    }
}
