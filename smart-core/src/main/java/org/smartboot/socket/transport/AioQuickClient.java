package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.Protocol;
import org.smartboot.socket.service.filter.SmartFilter;
import org.smartboot.socket.service.filter.impl.SmartFilterChainImpl;
import org.smartboot.socket.service.process.MessageProcessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * AIO实现的客户端服务
 * Created by seer on 2017/6/28.
 */
public class AioQuickClient<T> {
    private static final Logger LOGGER = LogManager.getLogger(AioSession.class);
    private AsynchronousSocketChannel socketChannel = null;
    /**
     * IO事件处理线程组
     */
    private AsynchronousChannelGroup asynchronousChannelGroup;

    /**
     * 服务配置
     */
    private IoServerConfig<T> config;

    public AioQuickClient() {
        this.config = new IoServerConfig<T>(false);
    }

    public void start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        this.socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        final AioSession<T> session = new AioSession<T>(socketChannel, config, new ReadCompletionHandler<T>(), new WriteCompletionHandler<T>(), new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters()));
        config.getProcessor().initSession(session);
        session.readFromChannel();
    }

    /**
     * 启动客户端Socket服务
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void start() throws IOException, ExecutionException, InterruptedException {
        this.asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        start(asynchronousChannelGroup);
    }

    /**
     * 停止客户端服务
     */
    public void shutdown() {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                LOGGER.catching(e);
            }
        }
        //仅Client内部创建的ChannelGroup需要shutdown
        if (asynchronousChannelGroup != null) {
            asynchronousChannelGroup.shutdown();
        }
    }

    /**
     * 设置远程连接的地址、端口
     *
     * @param host
     * @param port
     * @return
     */
    public AioQuickClient<T> connect(String host, int port) {
        this.config.setHost(host);
        this.config.setPort(port);
        return this;
    }

    /**
     * 设置协议对象
     *
     * @param protocol
     * @return
     */
    public AioQuickClient<T> setProtocol(Protocol<T> protocol) {
        this.config.setProtocol(protocol);
        return this;
    }

    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     * @return
     */
    public AioQuickClient<T> setFilters(SmartFilter<T>[] filters) {
        this.config.setFilters(filters);
        return this;
    }

    /**
     * 设置消息处理器
     *
     * @param processor
     * @return
     */
    public AioQuickClient<T> setProcessor(MessageProcessor<T> processor) {
        this.config.setProcessor(processor);
        return this;
    }

}
