package net.vinote.smart.socket.transport.aio;

import net.vinote.smart.socket.protocol.ProtocolFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.process.AbstractAIOServerProcessor;
import net.vinote.smart.socket.transport.IoServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * Created by zhengjunwei on 2017/6/28.
 */
public class AioQuickServer<T> implements IoServer {
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private IoServerConfig<T> config;

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

    public AioQuickServer<T> setProtocolFactory(ProtocolFactory<T> protocolFactory) {
        this.config.setProtocolFactory(protocolFactory);
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
    public AioQuickServer<T> setProcessor(AbstractAIOServerProcessor<T> processor) {
        this.config.setProcessor(processor);
        return this;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void start() throws IOException {
        asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });

        this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup).bind(new InetSocketAddress("localhost", 8888));
        this.config.getProcessor().init(4);
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(final AsynchronousSocketChannel channel, Object attachment) {
                System.out.println("new Connect:" + channel);
                serverSocketChannel.accept(attachment, this);
                final AioSession session = new AioSession(channel, config);
                config.getProcessor().initSession(session);
                session.registerReadHandler();

                System.out.println("finiash Connect:" + channel);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        });
    }

    @Override
    public void run() {

    }


    public static void main(String[] args) throws IOException {
        new AioQuickServer<Object>().start();
    }
}
