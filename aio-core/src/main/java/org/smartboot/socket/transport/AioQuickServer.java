/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioQuickServer.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AIO服务端。
 *
 * <h2>示例：</h2>
 * <p>
 * <pre>
 * public class IntegerServer {
 *     public static void main(String[] args) throws IOException {
 *         AioQuickServer<Integer> server = new AioQuickServer<Integer>(8888, new IntegerProtocol(), new IntegerServerProcessor());
 *         server.start();
 *     }
 * }
 * </pre>
 * </p>
 *
 * @author 三刀
 * @version V1.0.0
 */
public final class AioQuickServer {

    /**
     * asynchronousServerSocketChannel
     */
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    /**
     * asynchronousChannelGroup
     */
    private AsynchronousChannelGroup asynchronousChannelGroup;

    /**
     * 是否开启低内存模式
     */
    private boolean lowMemory = true;
    /**
     * 客户端服务配置。
     * <p>调用AioQuickClient的各setXX()方法，都是为了设置config的各配置项</p>
     */
    private final IoServerConfig config = new IoServerConfig();
    private static long threadSeqNumber;
    /**
     * write 内存池
     */
    private BufferPagePool writeBufferPool = null;
    /**
     * read 内存池
     */
    private BufferPagePool readBufferPool = null;


    /**
     * 设置服务端启动必要参数配置
     *
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public <T> AioQuickServer(int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
        config.setThreadNum(Runtime.getRuntime().availableProcessors());
    }

    /**
     * @param host             绑定服务端Host地址
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public <T> AioQuickServer(String host, int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        this(port, protocol, messageProcessor);
        config.setHost(host);
    }

    /**
     * 启动Server端的AIO服务
     *
     * @throws IOException IO异常
     */
    public void start() throws IOException {
        asynchronousChannelGroup = new EnhanceAsynchronousChannelProvider(lowMemory).openAsynchronousChannelGroup(config.getThreadNum(), r -> new Thread(r, "smart-socket:Thread-" + (threadSeqNumber++)));
        start(asynchronousChannelGroup);
    }

    /**
     * 内部启动逻辑
     *
     * @throws IOException IO异常
     */
    public void start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException {
        if (config.isBannerEnabled()) {
            System.out.println(IoServerConfig.BANNER + "\r\n :: smart-socket " + "::\t(" + IoServerConfig.VERSION + ") [port: " + config.getPort() + ", threadNum:" + config.getThreadNum() + "]");
            System.out.println("Technical Support:");
            System.out.println(" - Document: https://smartboot.tech]");
            System.out.println(" - Gitee: https://gitee.com/smartboot/smart-socket");
            System.out.println(" - Github: https://github.com/smartboot/smart-socket");
        }
        try {
            if (writeBufferPool == null) {
                this.writeBufferPool = BufferPagePool.DEFAULT_BUFFER_PAGE_POOL;
            }
            if (readBufferPool == null) {
                this.readBufferPool = BufferPagePool.DEFAULT_BUFFER_PAGE_POOL;
            }

            this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
            //set socket options
            if (config.getSocketOptions() != null) {
                for (Map.Entry<SocketOption<Object>, Object> entry : config.getSocketOptions().entrySet()) {
                    this.serverSocketChannel.setOption(entry.getKey(), entry.getValue());
                }
            }
            //bind host
            if (config.getHost() != null) {
                serverSocketChannel.bind(new InetSocketAddress(config.getHost(), config.getPort()), config.getBacklog());
            } else {
                serverSocketChannel.bind(new InetSocketAddress(config.getPort()), config.getBacklog());
            }

            startAcceptThread();
        } catch (IOException e) {
            shutdown();
            throw e;
        }
    }

    private void startAcceptThread() {
        Supplier<VirtualBuffer> readBufferSupplier = () -> readBufferPool.allocateBufferPage().allocate(config.getReadBufferSize());
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel channel, Void attachment) {
                try {
                    serverSocketChannel.accept(attachment, this);
                } catch (Throwable throwable) {
                    config.getProcessor().stateEvent(null, StateMachineEnum.ACCEPT_EXCEPTION, throwable);
                    failed(throwable, attachment);
                    serverSocketChannel.accept(attachment, this);
                } finally {
                    createSession(channel, readBufferSupplier);
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        });
    }

    /**
     * 为每个新建立的连接创建AIOSession对象
     *
     * @param channel 当前已建立连接通道
     */
    private void createSession(AsynchronousSocketChannel channel, Supplier<VirtualBuffer> readBufferSupplier) {
        //连接成功则构造AIOSession对象
        TcpAioSession session = null;
        AsynchronousSocketChannel acceptChannel = channel;
        try {
            if (config.getMonitor() != null) {
                acceptChannel = config.getMonitor().shouldAccept(channel);
            }
            if (acceptChannel != null) {
                acceptChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                session = new TcpAioSession(acceptChannel, this.config, writeBufferPool.allocateBufferPage(), readBufferSupplier);
            } else {
                config.getProcessor().stateEvent(null, StateMachineEnum.REJECT_ACCEPT, null);
                IOUtil.close(channel);
            }
        } catch (Exception e) {
            if (session == null) {
                IOUtil.close(channel);
            } else {
                session.close();
            }
            config.getProcessor().stateEvent(null, StateMachineEnum.INTERNAL_EXCEPTION, e);
        }
    }

    /**
     * 停止服务端
     */
    public void shutdown() {
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (asynchronousChannelGroup != null) {
            asynchronousChannelGroup.shutdown();
        }
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     * @return 当前AioQuickServer对象
     */
    public AioQuickServer setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    /**
     * 设置Socket的TCP参数配置。
     * <p>
     * AIO客户端的有效可选范围为：<br/>
     * 2. StandardSocketOptions.SO_RCVBUF<br/>
     * 4. StandardSocketOptions.SO_REUSEADDR<br/>
     * </p>
     *
     * @param socketOption 配置项
     * @param value        配置值
     * @param <V>          配置项类型
     * @return 当前AioQuickServer对象
     */
    public final <V> AioQuickServer setOption(SocketOption<V> socketOption, V value) {
        config.setOption(socketOption, value);
        return this;
    }

    /**
     * 设置服务工作线程数,设置数值必须大于等于2
     *
     * @param threadNum 线程数
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer setThreadNum(int threadNum) {
        if (threadNum <= 1) {
            throw new InvalidParameterException("threadNum must >= 2");
        }
        config.setThreadNum(threadNum);
        return this;
    }


    /**
     * 设置输出缓冲区容量
     *
     * @param bufferSize     单个内存块大小
     * @param bufferCapacity 内存块数量上限
     * @return 当前AioQuickServer对象
     */
    public AioQuickServer setWriteBuffer(int bufferSize, int bufferCapacity) {
        config.setWriteBufferSize(bufferSize);
        config.setWriteBufferCapacity(bufferCapacity);
        return this;
    }

    /**
     * 设置 backlog 大小
     *
     * @param backlog backlog大小
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer setBacklog(int backlog) {
        config.setBacklog(backlog);
        return this;
    }

    /**
     * 设置读写内存池。
     * 该方法适用于多个AioQuickServer、AioQuickClient共享内存池的场景，
     * <b>以获得更好的性能表现</b>
     *
     * @param bufferPool 内存池对象
     * @return 当前AioQuickServer对象
     */
    public AioQuickServer setBufferPagePool(BufferPagePool bufferPool) {
        return setBufferPagePool(bufferPool, bufferPool);
    }

    /**
     * 设置读写内存池。
     * 该方法适用于多个AioQuickServer、AioQuickClient共享内存池的场景，
     * <b>以获得更好的性能表现</b>
     *
     * @param readBufferPool  读内存池对象
     * @param writeBufferPool 写内存池对象
     * @return 当前AioQuickServer对象
     */
    public AioQuickServer setBufferPagePool(BufferPagePool readBufferPool, BufferPagePool writeBufferPool) {
        this.writeBufferPool = writeBufferPool;
        this.readBufferPool = readBufferPool;
        return this;
    }

    /**
     * 禁用低代码模式
     * @return
     */
    public AioQuickServer disableLowMemory() {
        this.lowMemory = false;
        return this;
    }
}
