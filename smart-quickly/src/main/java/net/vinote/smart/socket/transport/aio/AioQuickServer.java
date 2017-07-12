package net.vinote.smart.socket.transport.aio;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.service.process.AbstractServerDataGroupProcessor;
import net.vinote.smart.socket.transport.ChannelService;
import net.vinote.smart.socket.transport.TransportSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * Created by zhengjunwei on 2017/6/28.
 */
public class AioQuickServer<T> implements ChannelService {
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private QuicklyConfig<T> config;

    public AioQuickServer(final QuicklyConfig<T> config) {
        this.config = config;
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
        this.config.getProcessor().init(config);
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(final AsynchronousSocketChannel channel, Object attachment) {
                System.out.println("new Connect:" + channel);
                serverSocketChannel.accept(attachment, this);
                final TransportSession session = new AioSession(channel, config);
                session.setAttribute(AbstractServerDataGroupProcessor.SESSION_KEY, config.getProcessor().initSession(session));
                ByteBuffer buffer = session.flushReadBuffer();
                channel.read(buffer, attachment, new ReadCompletionHandler(channel, session));
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

    class ReadCompletionHandler implements CompletionHandler<Integer, Object> {
        AsynchronousSocketChannel channel;
        TransportSession session;

        public ReadCompletionHandler(AsynchronousSocketChannel channel, TransportSession session) {
            this.channel = channel;
            this.session = session;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            if (result == -1) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            ByteBuffer buffer = session.flushReadBuffer();
            channel.read(buffer, attachment, this);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            exc.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new AioQuickServer<Object>(null).start();
    }
}
