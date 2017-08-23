package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.Protocol;
import org.smartboot.socket.service.filter.SmartFilter;
import org.smartboot.socket.service.filter.SmartFilterChain;
import org.smartboot.socket.service.filter.impl.SmartFilterChainImpl;
import org.smartboot.socket.service.process.MessageProcessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhengjunwei on 2017/6/28.
 */
public class AioQuickServer<T> {
    private static final Logger LOGGER = LogManager.getLogger(AioQuickServer.class);
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private IoServerConfig<T> config;
    private ReadCompletionHandler readCompletionHandler = new ReadCompletionHandler();
    private WriteCompletionHandler writeCompletionHandler = new WriteCompletionHandler();
    /**
     * 消息过滤器
     */
    private SmartFilterChain<T> smartFilterChain;

    public AioQuickServer() {
        this.config = new IoServerConfig<T>(true);
    }

    /**
     * 设置服务绑定的端口
     *
     * @param port
     * @return
     */
    public AioQuickServer<T> bind(int port) {
        this.config.setPort(port);
        return this;
    }

    /**
     * 设置处理线程数量
     *
     * @param num
     * @return
     */
    public AioQuickServer<T> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }

    public AioQuickServer<T> setProtocol(Protocol<T> protocol) {
        this.config.setProtocol(protocol);
        return this;
    }

    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     * @return
     */
    public AioQuickServer<T> setFilters(SmartFilter<T>... filters) {
        this.config.setFilters(filters);
        return this;
    }

    /**
     * 设置消息处理器
     *
     * @param processor
     * @return
     */
    public AioQuickServer<T> setProcessor(MessageProcessor<T> processor) {
        this.config.setProcessor(processor);
        return this;
    }


    public void start() throws IOException {
        smartFilterChain = new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters());
        final AtomicInteger threadIndex = new AtomicInteger(0);
        asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(config.getThreadNum(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "AIO-Server-" + threadIndex.incrementAndGet());
            }
        });

        this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup).bind(new InetSocketAddress(config.getPort()), 1000);
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(final AsynchronousSocketChannel channel, Object attachment) {
                serverSocketChannel.accept(attachment, this);
                try {
                    channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                    channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                } catch (IOException e) {
                    LOGGER.catching(e);
                }
                AioSession session = new AioSession(channel, config, readCompletionHandler, writeCompletionHandler, smartFilterChain);
                config.getProcessor().initSession(session);
                session.registerReadHandler();
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                LOGGER.warn(exc);
            }
        });
    }

    public void shutdown() {
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        asynchronousChannelGroup.shutdown();
    }
}
