/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpBootstrap.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.buffer.BufferFactory;
import org.smartboot.socket.buffer.BufferPagePool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * UDP服务启动类
 *
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
public class UdpBootstrap {

    /**
     * 内存池
     */
    private BufferPagePool bufferPool;
    private BufferPagePool innerBufferPool = null;
    /**
     * 服务配置
     */
    private final IoServerConfig config = new IoServerConfig();

    private Worker worker;
    private boolean innerWorker = false;


    public <Request> UdpBootstrap(Protocol<Request> protocol, MessageProcessor<Request> messageProcessor, Worker worker) {
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
        this.worker = worker;
    }

    public <Request> UdpBootstrap(Protocol<Request> protocol, MessageProcessor<Request> messageProcessor) {
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    /**
     * 开启一个UDP通道，端口号随机
     *
     * @return UDP通道
     */
    public UdpChannel open() throws IOException {
        return open(0);
    }

    /**
     * 开启一个UDP通道
     *
     * @param port 指定绑定端口号,为0则随机指定
     */
    public UdpChannel open(int port) throws IOException {
        return open(null, port);
    }

    /**
     * 开启一个UDP通道
     *
     * @param host 绑定本机地址
     * @param port 指定绑定端口号,为0则随机指定
     */
    public UdpChannel open(String host, int port) throws IOException {
        //启动线程服务
        if (worker == null) {
            initWorker();
        }

        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        if (port > 0) {
            InetSocketAddress inetSocketAddress = host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
            channel.socket().bind(inetSocketAddress);
        }
        return new UdpChannel(channel, worker, config, bufferPool.allocateBufferPage());
    }

    private synchronized void initWorker() throws IOException {
        if (worker != null) {
            return;
        }

        // 增加广告说明
        if (config.isBannerEnabled()) {
            System.out.println(IoServerConfig.BANNER + "\r\n :: smart-socket[udp] ::\t(" + IoServerConfig.VERSION + ")");
        }

        if (bufferPool == null) {
            this.bufferPool = config.getBufferFactory().create();
            this.innerBufferPool = bufferPool;
        }
        innerWorker = true;
        worker = new Worker(bufferPool, config.getThreadNum());
    }


    public void shutdown() {
        if (innerWorker) {
            worker.shutdown();
        }
        if (innerBufferPool != null) {
            innerBufferPool.release();
        }
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final UdpBootstrap setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }


    /**
     * 设置线程大小
     *
     * @param num 线程数
     */
    public final UdpBootstrap setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }


    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     * @return 当前AioQuickServer对象
     */
    public final UdpBootstrap setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    /**
     * 设置内存池。
     * 通过该方法设置的内存池，在AioQuickServer执行shutdown时不会触发内存池的释放。
     * 该方法适用于多个AioQuickServer、AioQuickClient共享内存池的场景。
     * <b>在启用内存池的情况下会有更好的性能表现</b>
     *
     * @param bufferPool 内存池对象
     * @return 当前AioQuickServer对象
     */
    public final UdpBootstrap setBufferPagePool(BufferPagePool bufferPool) {
        this.bufferPool = bufferPool;
        this.config.setBufferFactory(BufferFactory.DISABLED_BUFFER_FACTORY);
        return this;
    }

    /**
     * 设置内存池的构造工厂。
     * 通过工厂形式生成的内存池会强绑定到当前UdpBootstrap对象，
     * 在UdpBootstrap执行shutdown时会释放内存池。
     * <b>在启用内存池的情况下会有更好的性能表现</b>
     *
     * @param bufferFactory 内存池工厂
     * @return 当前AioQuickServer对象
     */
    public final UdpBootstrap setBufferFactory(BufferFactory bufferFactory) {
        this.config.setBufferFactory(bufferFactory);
        this.bufferPool = null;
        return this;
    }


}

