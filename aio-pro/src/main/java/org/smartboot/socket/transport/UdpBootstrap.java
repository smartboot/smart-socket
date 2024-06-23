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
     * write 内存池
     */
    private BufferPagePool writeBufferPool = null;
    /**
     * read 内存池
     */
    private BufferPagePool readBufferPool = null;
    /**
     * 服务配置
     */
    private final IoServerConfig config = new IoServerConfig();

    private Worker worker;
    private boolean innerWorker = false;


    public <Request> UdpBootstrap(Protocol<Request> protocol, MessageProcessor<Request> messageProcessor, Worker worker) {
        this(protocol, messageProcessor);
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
        // 增加广告说明
        if (config.isBannerEnabled()) {
            System.out.println(IoServerConfig.BANNER + "\r\n :: smart-socket[udp] ::\t(" + IoServerConfig.VERSION + ")");
        }
        //初始化内存池
        if (writeBufferPool == null) {
            this.writeBufferPool = BufferPagePool.DEFAULT_BUFFER_PAGE_POOL;
        }
        if (readBufferPool == null) {
            this.readBufferPool = BufferPagePool.DEFAULT_BUFFER_PAGE_POOL;
        }

        // 初始化工作线程
        if (worker == null) {
            innerWorker = true;
            worker = new Worker(readBufferPool.allocateBufferPage(), writeBufferPool, config.getThreadNum());
        }


        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        if (port > 0) {
            InetSocketAddress inetSocketAddress = host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
            channel.socket().bind(inetSocketAddress);
        }
        return new UdpChannel(channel, worker, config, writeBufferPool.allocateBufferPage());
    }

    private synchronized void initWorker() throws IOException {
        if (worker != null) {
            return;
        }


    }


    public void shutdown() {
        if (innerWorker) {
            worker.shutdown();
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
        this.readBufferPool = bufferPool;
        this.writeBufferPool = bufferPool;
        return this;
    }

}

