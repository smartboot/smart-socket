package net.vinote.smart.socket.transport.nio;

import net.vinote.smart.socket.enums.ChannelServiceStatusEnum;
import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.service.process.AbstractServerDataGroupProcessor;
import net.vinote.smart.socket.util.QuicklyConfig;
import net.vinote.smart.socket.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidParameterException;
import java.util.Set;

/**
 * @author Seer
 * @version NioQuickClient.java, v 0.1 2015年3月20日 下午2:55:08 Seer Exp.
 */
public class NioQuickClient<T> extends AbstractIoServer<T> {
    private static Logger logger = LogManager.getLogger(NioQuickClient.class);
    /**
     * Socket连接锁,用于监听连接超时
     */
    private final Object conenctLock = new Object();

    /**
     * 客户端会话信息
     */
    private NioSession<T> session;

    private SocketChannel socketChannel;

    /**
     * @param config
     */
    public NioQuickClient(final QuicklyConfig<T> config) {
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
    void acceptConnect(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.finishConnect();
        key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
        // 自动修复链路
        if (session != null && config.isAutoRecover()) {
//            session.initBaseChannelInfo(key);
            logger.info("Socket link has been recovered!");
        } else {
            session = new NioSession<T>(key, config);
            logger.info("success connect to " + channel.socket().getRemoteSocketAddress().toString());
            session.setAttribute(AbstractServerDataGroupProcessor.SESSION_KEY, config.getProcessor().initSession(session));
            session.sessionWriteThread = writeThreads[0];
            session.sessionReadThread = selectReadThread();
            key.attach(session);
            synchronized (conenctLock) {
                conenctLock.notifyAll();
            }
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see net.vinote.smart.socket.transport.AbstractChannelService#
     * exceptionInSelectionKey(java.nio.channels.SelectionKey,
     * java.lang.Exception)
     */
    @Override
    void exceptionInSelectionKey(final SelectionKey key, final Exception e) throws Exception {
        throw e;
    }

    @Override
    void exceptionInSelector(final Exception e) {
        logger.catching(e);
        if (ChannelServiceStatusEnum.RUNING == status && config.isAutoRecover()) {
            restart();
        } else {
            shutdown();
        }
    }

    private void restart() {
        try {
            final Set<SelectionKey> keys = selector.keys();
            if (keys != null) {
                for (final SelectionKey key : keys) {
                    key.cancel();
                }
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort()));
            logger.info("Client " + config.getLocalIp() + " will reconnect to [IP:" + config.getHost() + " ,Port:"
                    + config.getPort() + "]");
        } catch (final IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.vinote.smart.socket.transport.ChannelService#shutdown()
     */
    public final void shutdown() {
        updateServiceStatus(ChannelServiceStatusEnum.STOPPING);
        config.getProcessor().shutdown();
        try {
            selector.close();
            selector.wakeup();
        } catch (final IOException e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            socketChannel.close();
        } catch (final IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.vinote.smart.socket.transport.ChannelService#start()
     */
    public final void start() {
        try {
            checkStart();
            assertAbnormalStatus();
            updateServiceStatus(ChannelServiceStatusEnum.STARTING);
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort()));
            serverThread = new Thread(this, "QuickClient-" + hashCode());
            serverThread.start();
            socketChannel.socket().setSoTimeout(config.getTimeout());

            if (session != null) {
                return;
            }
            synchronized (conenctLock) {
                if (session != null) {
                    return;
                }
                try {
                    conenctLock.wait(config.getTimeout());
                } catch (final InterruptedException e) {
                    logger.warn("", e);
                }
            }

        } catch (final IOException e) {
            logger.warn("", e);
        }
    }

    @Override
    void checkStart() {
        super.checkStart();
        if (!config.isClient()) {
            throw new StatusException("invalid quciklyConfig");
        }
        if (StringUtils.isBlank(config.getHost())) {
            throw new InvalidParameterException("invalid host " + config.getHost());
        }
    }

    @Override
    protected void notifyWhenUpdateStatus(ChannelServiceStatusEnum status) {
        if (status == null) {
            return;
        }
        switch (status) {
            case RUNING:
                try {
                    config.getProcessor().init(config);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;

            default:
                break;
        }
    }
}
