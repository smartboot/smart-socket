package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.Protocol;
import org.smartboot.socket.service.MessageProcessor;
import org.smartboot.socket.service.SmartFilter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AIO服务端
 * Created by 三刀 on 2017/6/28.
 */
public class AioQuickServer<T> {
    private static final Logger LOGGER = LogManager.getLogger(AioQuickServer.class);
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private IoServerConfig<T> config = new IoServerConfig<T>();
    private AioCompletionHandler aioCompletionHandler = new AioCompletionHandler();

    public void start() throws IOException {
        final AtomicInteger threadIndex = new AtomicInteger(0);
        asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(config.getThreadNum(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "AIO-Thread-" + threadIndex.incrementAndGet());
            }
        });

        this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup).bind(new InetSocketAddress(config.getPort()), 1000);
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(final AsynchronousSocketChannel channel, Object attachment) {
                serverSocketChannel.accept(attachment, this);
                //连接成功则构造AIOSession对象
                new AioSession<T>(channel, config, aioCompletionHandler,true);
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
            LOGGER.catching(e);
        }
        asynchronousChannelGroup.shutdown();
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

    /**
     * 设置输出队列缓冲区长度
     *
     * @param size
     * @return
     */
    public AioQuickServer<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

}
