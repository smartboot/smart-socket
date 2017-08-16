package net.vinote.smart.socket.transport.aio;

import net.vinote.smart.socket.protocol.ProtocolFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.process.AbstractAioClientDataProcessor;
import net.vinote.smart.socket.transport.IoServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

/**
 * Created by zhengjunwei on 2017/6/28.
 */
public class AioQuickClient<T> implements IoServer {
    private AsynchronousSocketChannel socketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private IoServerConfig<T> config;

    public AioQuickClient(AsynchronousChannelGroup asynchronousChannelGroup) {
        this.config = new IoServerConfig<T>(false);
        this.asynchronousChannelGroup = asynchronousChannelGroup;
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

    public AioQuickClient<T> setProtocolFactory(ProtocolFactory<T> protocolFactory) {
        this.config.setProtocolFactory(protocolFactory);
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
    public AioQuickClient<T> setProcessor(AbstractAioClientDataProcessor<T> processor) {
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

    @Override
    public void shutdown() {
        config.getProcessor().shutdown();
    }

    @Override
    public void start() throws IOException, ExecutionException, InterruptedException {
        this.socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        this.config.getProcessor().init(config.getThreadNum());
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        final AioSession session = new AioSession(socketChannel, config, new ReadCompletionHandler(),new WriteCompletionHandler());
        config.getProcessor().initSession(session);
        session.registerReadHandler(true);
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException();
    }
}
