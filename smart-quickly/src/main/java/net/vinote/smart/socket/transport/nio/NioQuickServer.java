package net.vinote.smart.socket.transport.nio;

import net.vinote.smart.socket.enums.ChannelServiceStatusEnum;
import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.util.QuicklyConfig;
import net.vinote.smart.socket.util.StringUtils;
import net.vinote.smart.socket.service.process.AbstractServerDataGroupProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * NIO服务器
 *
 * @author Seer
 */
public final class NioQuickServer<T> extends AbstractIoServer<T> {
    private static Logger logger = LogManager.getLogger(NioQuickServer.class);

    private ServerSocketChannel server;

    /**
     * 客户端accpet处理线程池
     */
    private Executor acceptExecutor = Executors.newSingleThreadExecutor();

    public NioQuickServer(final QuicklyConfig<T> config) {
        super(config);
    }

    /**
     * 接受并建立客户端与服务端的连接
     *
     * @param key
     * @param selector
     * @throws IOException
     */
    @Override
    protected void acceptConnect(final SelectionKey key, final Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();

        int batchNum = 1000;

        while (key.isAcceptable() && batchNum-- > 0) {
            final SocketChannel socketChannel = serverChannel.accept();
            if (socketChannel == null) {
                break;
            }
            socketChannel.configureBlocking(false);
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 32 * 1024);
            socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 32 * 1024);
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            final SelectionKey socketKey = socketChannel.register(selector, 0);
            acceptExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        NioSession<T> nioSession = new NioSession<T>(socketKey, config);
                        socketKey.attach(nioSession);
                        nioSession.sessionReadThread = selectReadThread();
                        nioSession.sessionWriteThread = selectWriteThread();
                        nioSession.setAttribute(AbstractServerDataGroupProcessor.SESSION_KEY, config.getProcessor().initSession(nioSession));
                        socketKey.interestOps(SelectionKey.OP_READ);
                        socketChannel.finishConnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 从管道流中读取数据
     *
     * @param key
     * @param attach
     * @throws IOException
     */

//    protected void readFromChannel(SelectionKey key, NioSession attach) throws IOException {
//        SessionReadThread readThread = attach.sessionReadThread;
//        //先取消读关注
////        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
//        readThread.notifySession(key);
//    }

    @Override
    protected void exceptionInSelectionKey(SelectionKey key, final Exception e) throws Exception {
        logger.warn("Close Channel because of Exception", e);
        final Object att = key.attach(null);
        if (att instanceof NioSession) {
            ((NioSession) att).close();
        }
        key.channel().close();
        logger.info("close connection " + key.channel());
        key.cancel();
    }

    @Override
    protected void exceptionInSelector(Exception e) {
        logger.warn(e.getMessage(), e);
    }


    public void shutdown() {
        updateServiceStatus(ChannelServiceStatusEnum.STOPPING);
        config.getProcessor().shutdown();
        try {
            if (selector != null) {
                selector.close();
                selector.wakeup();
            }
        } catch (final IOException e1) {
            logger.warn("", e1);
        }
        try {
            server.close();
        } catch (final IOException e) {
            logger.warn("", e);
        }
    }

    public void start() throws IOException {
        try {
            checkStart();
            assertAbnormalStatus();
            updateServiceStatus(ChannelServiceStatusEnum.STARTING);
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            InetSocketAddress address = null;
            if (StringUtils.isBlank(config.getLocalIp())) {
                address = new InetSocketAddress(config.getPort());
            } else {
                address = new InetSocketAddress(config.getLocalIp(), config.getPort());
            }
            server.socket().bind(address);
            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            serverThread = new Thread(this, "Nio-Server");
            serverThread.start();
        } catch (final IOException e) {
            logger.catching(e);
            shutdown();
            throw e;
        }
    }

    @Override
    protected void notifyWhenUpdateStatus(ChannelServiceStatusEnum status) {
        if (status == null) {
            return;
        }
        switch (status) {
            case RUNING:
                logger.info("Running with " + config.getPort() + " port");
                config.getProcessor().init(config);
                break;

            default:
                break;
        }
    }

    @Override
    void checkStart() {
        super.checkStart();
        if (!config.isServer()) {
            throw new StatusException("invalid quciklyConfig");
        }
    }

}
