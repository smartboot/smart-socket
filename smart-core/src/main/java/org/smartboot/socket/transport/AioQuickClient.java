package org.smartboot.socket.transport;

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
 * Created by zhengjunwei on 2017/6/28.
 */
public class AioQuickClient<T> {
    private AsynchronousSocketChannel socketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private IoServerConfig<T> config;

    public AioQuickClient(AsynchronousChannelGroup asynchronousChannelGroup) {
        this.config = new IoServerConfig<T>(false);
        this.asynchronousChannelGroup = asynchronousChannelGroup;
    }

    public AioQuickClient() throws IOException {
        this.config = new IoServerConfig<T>(false);
        this.asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
    }

    public void start() throws IOException, ExecutionException, InterruptedException {
        this.socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        final AioSession session = new AioSession(socketChannel, config, new ReadCompletionHandler(), new WriteCompletionHandler(), new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters()));
        config.getProcessor().initSession(session);
        session.registerReadHandler();
    }

    public void shutdown() {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
     * 设置协议对象的构建工厂
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

    /**
     * 定义同步消息的超时时间
     *
     * @param timeout
     * @return
     */
    public AioQuickClient<T> setTimeout(int timeout) {
        this.config.setTimeout(timeout);
        return this;
    }


}
