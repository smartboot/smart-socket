package org.smartboot.socket.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/15
 */
public class UdpBootstrap<T> implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpBootstrap.class);
    /**
     * 服务状态
     */
    private volatile IoServerStatusEnum status = IoServerStatusEnum.Init;
    private Selector selector;

    private UdpChannel<T> udpChannel;
    private IoServerConfig<T> config = new IoServerConfig<>();

    public UdpBootstrap(Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    public UdpBootstrap(Protocol<T> protocol, MessageProcessor<T> messageProcessor, int port) {
        this(protocol, messageProcessor);
        config.setPort(port);
    }


    public void shutdown() {

    }

    public UdpChannel<T> start() throws IOException {
        try {
//            checkStart();
//            assertAbnormalStatus();
            updateServiceStatus(IoServerStatusEnum.STARTING);
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            if (config.getPort() > 0) {
                channel.socket().bind(new InetSocketAddress(config.getPort()));
            }
            selector = Selector.open();
            SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ);

            this.udpChannel = new UdpChannel<>(channel, selectionKey, config);
            Thread serverThread = new Thread(this, "Nio-Server");
            serverThread.start();
        } catch (final IOException e) {
            throw e;
        }
        return udpChannel;
    }

    final void updateServiceStatus(final IoServerStatusEnum status) {
        this.status = status;
//        notifyWhenUpdateStatus(status);
    }

    @Override
    public void run() {
        updateServiceStatus(IoServerStatusEnum.RUNING);
        // 通过检查状态使之一直保持服务状态
        while (IoServerStatusEnum.RUNING == status) {
            try {
                LOGGER.info("running");
                running();
            } catch (ClosedSelectorException e) {
                updateServiceStatus(IoServerStatusEnum.Abnormal);// Selector关闭触发服务终止
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateServiceStatus(IoServerStatusEnum.STOPPED);
        LOGGER.info("Channel is stop!");
    }

    /**
     * 运行channel服务
     *
     * @throws IOException
     * @throws Exception
     */
    private void running() throws IOException, Exception {
        // 优先获取SelectionKey,若无关注事件触发则阻塞在selector.select(),减少select被调用次数
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        if (selectionKeys.isEmpty()) {
            selector.select();
        }
        Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
        // 执行本次已触发待处理的事件
        while (keyIterator.hasNext()) {
            final SelectionKey key = keyIterator.next();
            try {
                // 读取客户端数据
                if (key.isReadable()) {
                    udpChannel.doRead();
                } else if (key.isWritable()) {
                    udpChannel.doWrite();
                } else {
                    LOGGER.warn("奇怪了...");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        selectionKeys.clear();
    }

}
