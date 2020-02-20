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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
 * @param <T> 消息对象类型
 * @author 三刀
 * @version V1.0.0
 */
public class AioQuickServer<T> {
    /**
     * Server端服务配置。
     * <p>调用AioQuickServer的各setXX()方法，都是为了设置config的各配置项</p>
     */
    protected IoServerConfig<T> config = new IoServerConfig<>();
    /**
     * 内存池
     */
    protected BufferPagePool bufferPool;
    /**
     * 读回调事件处理
     */
    protected ReadCompletionHandler<T> aioReadCompletionHandler;
    /**
     * 写回调事件处理
     */
    protected WriteCompletionHandler<T> aioWriteCompletionHandler;
    /**
     * 连接会话实例化Function
     */
    private Function<AsynchronousSocketChannel, TcpAioSession<T>> aioSessionFunction;
    /**
     * asynchronousServerSocketChannel
     */
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    /**
     * asynchronousChannelGroup
     */
    private AsynchronousChannelGroup asynchronousChannelGroup;

    private boolean acceptRunning = true;


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
     * @throws IOException IO异常
     */
    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            System.out.println(IoServerConfig.BANNER + "\r\n :: smart-socket ::\t(" + IoServerConfig.VERSION + ")");
        }
        start0(channel -> new TcpAioSession<T>(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, bufferPool.allocateBufferPage()));
    }

    /**
     * 内部启动逻辑
     *
     * @param aioSessionFunction 实例化会话的Function
     * @throws IOException IO异常
     */
    protected final void start0(Function<AsynchronousSocketChannel, TcpAioSession<T>> aioSessionFunction) throws IOException {
        checkAndResetConfig();

        try {
            aioWriteCompletionHandler = new WriteCompletionHandler<>();
            this.bufferPool = new BufferPagePool(config.getBufferPoolPageSize(), config.getBufferPoolPageNum(), config.getBufferPoolSharedPageSize(), config.isBufferPoolDirect());
            this.aioSessionFunction = aioSessionFunction;

            aioReadCompletionHandler = new ConcurrentReadCompletionHandler<>(new Semaphore(config.getThreadNum() - 1));
            asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(config.getThreadNum(), r -> bufferPool.newThread(r, "smart-socket:Worker-"));

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
        System.out.println("smart-socket server started on port " + config.getPort() + ",threadNum:" + config.getThreadNum());
        System.out.println("smart-socket server config is " + config);
    }

    private void startAcceptThread() {
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel channel, Void attachment) {
                try {
                    serverSocketChannel.accept(attachment, this);
                } catch (Throwable throwable) {
                    config.getProcessor().stateEvent(null, StateMachineEnum.ACCEPT_EXCEPTION, throwable);
                    failed(throwable, attachment);
                    serverSocketChannel.accept(attachment, this);
                }
                createSession(channel);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        });

//        Thread acceptThread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                Future<AsynchronousSocketChannel> nextFuture = serverSocketChannel.accept();
//                while (acceptRunning) {
//                    try {
//                        AsynchronousSocketChannel channel = nextFuture.get();
//                        nextFuture = serverSocketChannel.accept();
//                        createSession(channel);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        config.getProcessor().stateEvent(null, StateMachineEnum.ACCEPT_EXCEPTION, e);
//                        nextFuture = serverSocketChannel.accept();
//                    }
//                }
//            }
//        }, "smart-socket:accept");
//        acceptThread.setDaemon(true);
//        acceptThread.start();
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
        if (config.getBufferPoolPageNum() <= 0) {
            config.setBufferPoolPageNum(config.getThreadNum());
        }
        //内存页数量不可多于线程数，会造成内存浪费
        if (config.getBufferPoolPageNum() > config.getThreadNum()) {
            throw new RuntimeException("bufferPoolPageNum=" + config.getBufferPoolPageNum() + " can't greater than threadNum=" + config.getThreadNum());
        }
        //内存块不可大于内存页
        if (config.getBufferPoolChunkSize() > config.getBufferPoolPageSize()) {
            throw new RuntimeException("bufferPoolChunkSize=" + config.getBufferPoolChunkSize() + " can't greater than bufferPoolPageSize=" + config.getBufferPoolPageSize());
        }
        //read缓冲区不可大于内存页
        if (config.getReadBufferSize() > config.getBufferPoolPageSize()) {
            throw new RuntimeException("readBufferSize=" + config.getReadBufferSize() + " can't greater than bufferPoolPageSize=" + config.getBufferPoolPageSize());
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
            if (config.getMonitor() == null || config.getMonitor().shouldAccept(channel)) {
                session = aioSessionFunction.apply(channel);
                session.initSession();
            } else {
                config.getProcessor().stateEvent(null, StateMachineEnum.REJECT_ACCEPT, null);
                IOUtil.close(channel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (session == null) {
                IOUtil.close(channel);
            } else {
                session.close();
            }
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
            e.printStackTrace();
        }

        if (!asynchronousChannelGroup.isTerminated()) {
            try {
                asynchronousChannelGroup.shutdownNow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            asynchronousChannelGroup.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (bufferPool != null) {
            bufferPool.release();
            bufferPool = null;
        }
        aioReadCompletionHandler.shutdown();
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     * @return 当前AioQuickServer对象
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
     * @param <V>          配置项类型
     * @return 当前AioQuickServer对象
     */
    public final <V> AioQuickServer<T> setOption(SocketOption<V> socketOption, V value) {
        config.setOption(socketOption, value);
        return this;
    }

    /**
     * 设置write缓冲区容量
     *
     * @param writeQueueCapacity 缓存区容量
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer<T> setWriteQueueCapacity(int writeQueueCapacity) {
        config.setWriteQueueCapacity(writeQueueCapacity);
        return this;
    }

    /**
     * 设置服务工作线程数,设置数值必须大于等于2
     *
     * @param threadNum 线程数
     * @return 当前AioQuickServer对象
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
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer<T> setBufferPoolPageSize(int bufferPoolPageSize) {
        config.setBufferPoolPageSize(bufferPoolPageSize);
        return this;
    }

    /**
     * 设置内存页个数，多个内存页共同组成内存池。
     *
     * @param bufferPoolPageNum 内存页个数
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer<T> setBufferPoolPageNum(int bufferPoolPageNum) {
        config.setBufferPoolPageNum(bufferPoolPageNum);
        return this;
    }


    /**
     * 限制写操作时从内存页中申请内存块的大小
     *
     * @param bufferPoolChunkSizeLimit 内存块大小限制
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer<T> setBufferPoolChunkSize(int bufferPoolChunkSizeLimit) {
        config.setBufferPoolChunkSize(bufferPoolChunkSizeLimit);
        return this;
    }

    /**
     * 设置内存池是否使用直接缓冲区,默认：true
     *
     * @param isDirect true:直接缓冲区,false:堆内缓冲区
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer<T> setBufferPoolDirect(boolean isDirect) {
        config.setBufferPoolDirect(isDirect);
        return this;
    }

    /**
     * 设置共享内存页大小
     *
     * @param bufferPoolSharedPageSize 共享内存页大小
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer<T> setBufferPoolSharedPageSize(int bufferPoolSharedPageSize) {
        config.setBufferPoolSharedPageSize(bufferPoolSharedPageSize);
        return this;
    }

    /**
     * 设置 backlog 大小
     *
     * @param backlog backlog大小
     * @return 当前AioQuickServer对象
     */
    public final AioQuickServer<T> setBacklog(int backlog) {
        config.setBacklog(backlog);
        return this;
    }

}
