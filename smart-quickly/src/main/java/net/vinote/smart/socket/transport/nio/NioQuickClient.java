package net.vinote.smart.socket.transport.nio;

import net.vinote.smart.socket.enums.IoServerStatusEnum;
import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.protocol.ProtocolFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.process.AbstractClientDataProcessor;
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
    NioSession<T> nioSession;

    private SocketChannel socketChannel;

    public NioQuickClient() {
        super.init(new IoServerConfig<T>(false));
        this.config.setThreadNum(1);
    }

    /**
     * 设置远程连接的地址、端口
     *
     * @param host
     * @param port
     * @return
     */
    public NioQuickClient<T> connect(String host, int port) {
        this.config.setHost(host);
        this.config.setPort(port);
        return this;
    }

    public NioQuickClient<T> setProtocolFactory(ProtocolFactory<T> protocolFactory) {
        this.config.setProtocolFactory(protocolFactory);
        return this;
    }

    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     * @return
     */
    public NioQuickClient<T> setFilters(SmartFilter<T>[] filters) {
        this.config.setFilters(filters);
        return this;
    }

    /**
     * 设置消息处理器
     *
     * @param processor
     * @return
     */
    public NioQuickClient<T> setProcessor(AbstractClientDataProcessor<T> processor) {
        this.config.setProcessor(processor);
        return this;
    }

    /**
     * 定义同步消息的超时时间
     *
     * @param timeout
     * @return
     */
    public NioQuickClient<T> setTimeout(int timeout) {
        this.config.setTimeout(timeout);
        return this;
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
        nioSession = new NioSession<T>(key, config);
        logger.info("success connect to " + channel.socket().getRemoteSocketAddress().toString());
        nioSession.sessionWriteThread = writeThreads[0];
        nioSession.sessionReadThread = selectReadThread();
        config.getProcessor().initSession(nioSession);
        key.attach(nioSession);
        synchronized (conenctLock) {
            conenctLock.notifyAll();
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
        if (IoServerStatusEnum.RUNING == status && config.isAutoRecover()) {
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
        updateServiceStatus(IoServerStatusEnum.STOPPING);
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
            updateServiceStatus(IoServerStatusEnum.STARTING);
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort()));
            serverThread = new Thread(this, "QuickClient-" + hashCode());
            serverThread.start();
            socketChannel.socket().setSoTimeout(config.getTimeout());

            if (nioSession != null) {
                return;
            }
            synchronized (conenctLock) {
                if (nioSession != null) {
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
    protected void notifyWhenUpdateStatus(IoServerStatusEnum status) {
        if (status == null) {
            return;
        }
        switch (status) {
            case RUNING:
                try {
                    config.getProcessor().init(1);
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
