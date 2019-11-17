/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioQuickServer.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
public class AioQuickServer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AioQuickServer.class);
    /**
     * Server端服务配置。
     * <p>调用AioQuickServer的各setXX()方法，都是为了设置config的各配置项</p>
     */
    protected IoServerConfig<T> config = new IoServerConfig<>();
    protected BufferPagePool bufferPool;
    /**
     * 读回调事件处理
     */
    protected ReadCompletionHandler<T> aioReadCompletionHandler;
    /**
     * 写回调事件处理
     */
    protected WriteCompletionHandler<T> aioWriteCompletionHandler;
    private Function<AsynchronousSocketChannel, TcpAioSession<T>> aioSessionFunction;
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    /**
     * accept处理线程
     */
    private Thread acceptThread = null;
    /**
     * accept线程运行状态
     */
    private volatile boolean acceptRunning = true;

    /**
     * 设置服务端启动必要参数配置
     *
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioQuickServer(int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
        config.setThreadNum(Runtime.getRuntime().availableProcessors());
        setBufferPoolPageSize(1024 * 1024);
    }

    /**
     * @param host             绑定服务端Host地址
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioQuickServer(String host, int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        this(port, protocol, messageProcessor);
        config.setHost(host);
    }

    /**
     * 启动Server端的AIO服务
     *
     * @throws IOException
     */
    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            LOGGER.info(IoServerConfig.BANNER + "\r\n :: smart-socket ::\t(" + IoServerConfig.VERSION + ")");
        }
        start0(new Function<AsynchronousSocketChannel, TcpAioSession<T>>() {
            @Override
            public TcpAioSession<T> apply(AsynchronousSocketChannel channel) {
                return new TcpAioSession<T>(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, bufferPool.allocateBufferPage());
            }
        });
    }

    /**
     * 内部启动逻辑
     *
     * @throws IOException
     */
    protected final void start0(Function<AsynchronousSocketChannel, TcpAioSession<T>> aioSessionFunction) throws IOException {
        checkAndResetConfig();

        try {

            aioReadCompletionHandler = new ReadCompletionHandler<>(new AtomicInteger(config.getThreadNum() - 1));
            aioWriteCompletionHandler = new WriteCompletionHandler<>();

            this.bufferPool = new BufferPagePool(config.getBufferPoolPageSize(), config.getBufferPoolPageNum(), config.getBufferPoolChunkSize(), config.isBufferPoolDirect());
            this.aioSessionFunction = aioSessionFunction;

            asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(config.getThreadNum(), new ThreadFactory() {
                private byte index = 0;

                @Override
                public Thread newThread(Runnable r) {
                    return bufferPool.newThread(r, "smart-socket:Thread-" + (++index));
                }
            });
            this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
            //set socket options
            if (config.getSocketOptions() != null) {
                for (Map.Entry<SocketOption<Object>, Object> entry : config.getSocketOptions().entrySet()) {
                    this.serverSocketChannel.setOption(entry.getKey(), entry.getValue());
                }
            }
            //bind host
            if (config.getHost() != null) {
                serverSocketChannel.bind(new InetSocketAddress(config.getHost(), config.getPort()), 1000);
            } else {
                serverSocketChannel.bind(new InetSocketAddress(config.getPort()), 1000);
            }
            acceptThread = new Thread(new Runnable() {
                private NetMonitor<T> monitor = config.getMonitor();

                @Override
                public void run() {
                    Future<AsynchronousSocketChannel> nextFuture = serverSocketChannel.accept();
                    while (acceptRunning) {
                        try {
                            final AsynchronousSocketChannel channel = nextFuture.get();
                            nextFuture = serverSocketChannel.accept();
                            if (monitor == null || monitor.shouldAccept(channel)) {
                                createSession(channel);
                            } else {
                                config.getProcessor().stateEvent(null, StateMachineEnum.REJECT_ACCEPT, null);
                                LOGGER.warn("reject accept channel:{}", channel);
                                closeChannel(channel);
                            }
                        } catch (Exception e) {
                            LOGGER.error("AcceptThread Exception", e);
                        }

                    }
                }
            }, "smart-socket:AcceptThread");
            acceptThread.start();
        } catch (IOException e) {
            shutdown();
            throw e;
        }
        LOGGER.info("smart-socket server started on port {},threadNum:{}", config.getPort(), config.getThreadNum());
        LOGGER.info("smart-socket server config is {}", config);
    }

    /**
     * 检查配置项
     */
    private void checkAndResetConfig() {
        //确保单核CPU默认初始化至少2个线程
        if (config.getThreadNum() == 1) {
            config.setThreadNum(2);
        }
        //未指定内存页数量默认等同于线程数
        if (config.getBufferPoolPageNum() < 0) {
            config.setBufferPoolPageNum(config.getThreadNum());
        }
        //内存页数量不可多于线程数，会造成内存浪费
        if (config.getBufferPoolPageNum() > config.getThreadNum()) {
            throw new RuntimeException("bufferPoolPageNum=" + config.getBufferPoolPageNum() + " can't greater than threadNum=" + config.getThreadNum());
        }
    }

    /**
     * 为每个新建立的连接创建AIOSession对象
     *
     * @param channel 当前已建立连接通道
     */
    private void createSession(AsynchronousSocketChannel channel) {
        //连接成功则构造AIOSession对象
        TcpAioSession<T> session = null;
        try {
            session = aioSessionFunction.apply(channel);
            session.initSession();
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            if (session == null) {
                closeChannel(channel);
            } else {
                session.close();
            }
        }
    }

    private void closeChannel(AsynchronousSocketChannel channel) {
        try {
            channel.shutdownInput();
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        try {
            channel.shutdownOutput();
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        try {
            channel.close();
        } catch (IOException e) {
            LOGGER.debug("close channel exception", e);
        }
    }

    /**
     * 停止服务端
     */
    public final void shutdown() {
        acceptRunning = false;
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }

        if (!asynchronousChannelGroup.isTerminated()) {
            try {
                asynchronousChannelGroup.shutdownNow();
            } catch (IOException e) {
                LOGGER.error("shutdown exception", e);
            }
        }
        try {
            asynchronousChannelGroup.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("shutdown exception", e);
        }
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final AioQuickServer<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     */
    public final AioQuickServer<T> setBannerEnabled(boolean bannerEnabled) {
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
     * @return
     */
    public final <V> AioQuickServer<T> setOption(SocketOption<V> socketOption, V value) {
        config.setOption(socketOption, value);
        return this;
    }

    /**
     * 设置write缓冲区容量
     *
     * @param writeQueueCapacity
     * @return
     */
    public final AioQuickServer<T> setWriteQueueCapacity(int writeQueueCapacity) {
        config.setWriteQueueCapacity(writeQueueCapacity);
        return this;
    }

    /**
     * 设置服务工作线程数,设置数值必须大于等于2
     *
     * @param threadNum 线程数
     * @return
     */
    public final AioQuickServer<T> setThreadNum(int threadNum) {
        if (threadNum <= 1) {
            throw new InvalidParameterException("threadNum must >= 2");
        }
        config.setThreadNum(threadNum);
        return this;
    }

    /**
     * 设置单个内存页大小.多个内存页共同组成内存池
     *
     * @param bufferPoolPageSize 内存页大小
     * @return
     */
    public final AioQuickServer<T> setBufferPoolPageSize(int bufferPoolPageSize) {
        config.setBufferPoolPageSize(bufferPoolPageSize);
        return this;
    }

    /**
     * 设置内存页个数，多个内存页共同组成内存池。
     *
     * @param bufferPoolPageNum 内存页个数
     * @return
     */
    public final AioQuickServer<T> setBufferPoolPageNum(int bufferPoolPageNum) {
        config.setBufferPoolPageNum(bufferPoolPageNum);
        return this;
    }


    /**
     * 限制写操作时从内存页中申请内存块的大小
     *
     * @param bufferPoolChunkSizeLimit 内存块大小限制
     * @return
     */
    public final AioQuickServer<T> setBufferPoolChunkSize(int bufferPoolChunkSizeLimit) {
        config.setBufferPoolChunkSize(bufferPoolChunkSizeLimit);
        return this;
    }

    /**
     * 设置内存池是否使用直接缓冲区,默认：true
     *
     * @param isDirect true:直接缓冲区,false:堆内缓冲区
     * @return
     */
    public final AioQuickServer<T> setBufferPoolDirect(boolean isDirect) {
        config.setBufferPoolDirect(isDirect);
        return this;
    }
}
