package net.vinote.smart.socket.transport.nio;

import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.service.process.AbstractServerDataProcessor;
import net.vinote.smart.socket.transport.TransportSession;
import net.vinote.smart.socket.transport.enums.ChannelServiceStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * NIO服务器
 *
 * @author Seer
 */
public final class NioQuickServer<T> extends AbstractChannelService<T> {
    private Logger logger = LogManager.getLogger(NioQuickServer.class);
    private ServerSocketChannel server;
    //数据读取线程
    private SessionReadThread[] readThreads;

    public NioQuickServer(final QuicklyConfig<T> config) {
        super(config);
        readThreads = new SessionReadThread[config.getThreadNum()];
        for (int i = 0; i < readThreads.length; i++) {
            readThreads[i] = new SessionReadThread();
            readThreads[i].setName("SessionReadThread-" + System.currentTimeMillis());
            readThreads[i].start();
        }
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
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);
        SelectionKey socketKey = socketChannel.register(selector, SelectionKey.OP_READ);
        NioSession<T> nioSession = new NioSession<T>(socketKey, config);
        socketKey.attach(new NioAttachment(nioSession));
        socketChannel.finishConnect();
        nioSession.setAttribute(AbstractServerDataProcessor.SESSION_KEY, config.getProcessor().initSession(nioSession));
        System.out.println(socketChannel);
    }

    /**
     * 从管道流中读取数据
     *
     * @param key
     * @param attach
     * @throws IOException
     */
    protected void readFromChannel(SelectionKey key, NioAttachment attach) throws IOException {
        SessionReadThread readThread = attach.getSession().getAttribute(TransportSession.DATA_READ_THREAD);
        //线程选举
        if (readThread == null) {
            int index = 0;
            for (int i = readThreads.length - 1; i > 0; i--) {
                if (readThreads[i].getConnectNums() < readThreads[index].getConnectNums()) {
                    index = i;
                }
            }
            readThread = readThreads[index];
            attach.getSession().setAttribute(TransportSession.DATA_READ_THREAD, readThread);
        }
        readThread.notifySession(key);
    }

    @Override
    protected void exceptionInSelectionKey(SelectionKey key, final Exception e) throws Exception {
        logger.warn("Close Channel because of Exception", e);
        final Object att = key.attach(null);
        if (att instanceof NioAttachment) {
            ((NioAttachment) att).getSession().close();
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
