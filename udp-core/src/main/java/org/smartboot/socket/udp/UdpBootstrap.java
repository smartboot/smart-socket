package org.smartboot.socket.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.ring.EventFactory;
import org.smartboot.socket.buffer.ring.RingBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * UDP服务启动类
 *
 * @author 三刀
 * @version V1.0 , 2019/8/17
 */
public class UdpBootstrap<T> implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpBootstrap.class);
    /**
     * 服务状态
     */
    private volatile IoServerStatusEnum status = IoServerStatusEnum.Init;
    /**
     * 多路复用器
     */
    private Selector selector;

    private IoServerConfig<T> config = new IoServerConfig<>();
    private volatile boolean threadStarted = false;
    /**
     * 已完成解码待业务处理的消息集合
     */
    private RingBuffer<ReadEvent<T>>[] readRingBuffers;

    private ByteBuffer readBuffer;

    private EventFactory<ReadEvent<T>> factory = new EventFactory<ReadEvent<T>>() {
        @Override
        public ReadEvent<T> newInstance() {
            return new ReadEvent<>();
        }

        @Override
        public void restEntity(ReadEvent<T> entity) {
            entity.setMessage(null);
            entity.setRemote(null);
            entity.setChannel(null);
        }
    };

    public UdpBootstrap(Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    public void shutdown() {
        try {
            if (selector != null) {
                selector.close();
                selector = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UdpChannel<T> start() throws IOException {
        return start(0);
    }

    /**
     * 启动服务
     *
     * @param port
     * @return
     * @throws IOException
     */
    public UdpChannel<T> start(int port) throws IOException {

        if (selector == null) {
            synchronized (this) {
                if (selector == null) {
                    selector = Selector.open();
                }
            }
        }

        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        if (port > 0) {
            channel.socket().bind(new InetSocketAddress(port));
        }

        SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ);
        UdpChannel<T> udpChannel = new UdpChannel<>(channel, selectionKey, config);
        selectionKey.attach(udpChannel);

        if (!threadStarted) {
            synchronized (this) {
                if (!threadStarted) {
                    readBuffer = ByteBuffer.allocate(config.getReadBufferSize());

                    updateServiceStatus(IoServerStatusEnum.STARTING);
                    Thread serverThread = new Thread(this, "Nio-Server");
                    serverThread.start();

                    readRingBuffers = new RingBuffer[config.getThreadNum()];
                    for (int i = 0; i < config.getThreadNum(); i++) {
                        final RingBuffer<ReadEvent<T>> ringBuffer = readRingBuffers[i] = new RingBuffer<>(1024, factory);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (IoServerStatusEnum.RUNING == status) {
                                    try {
                                        int index = ringBuffer.nextReadIndex();
                                        ReadEvent<T> event = ringBuffer.get(index);
                                        SocketAddress remote = event.getRemote();
                                        UdpChannel<T> channel = event.getChannel();
                                        T message = event.getMessage();
                                        ringBuffer.publishReadIndex(index);
                                        config.getProcessor().process(channel, remote, message);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).start();
                    }
                    threadStarted = true;
                }
            }
        }
        return udpChannel;
    }

    private void updateServiceStatus(final IoServerStatusEnum status) {
        this.status = status;
//        notifyWhenUpdateStatus(status);
    }

    @Override
    public void run() {
        updateServiceStatus(IoServerStatusEnum.RUNING);
        // 通过检查状态使之一直保持服务状态
        while (IoServerStatusEnum.RUNING == status) {
            try {
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
            UdpChannel<T> udpChannel = (UdpChannel<T>) key.attachment();
            try {
                // 读取客户端数据
                if (key.isReadable()) {
                    udpChannel.doRead(readBuffer, readRingBuffers);
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

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final UdpBootstrap<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }


    /**
     * 设置线程大小
     *
     * @param num
     */
    public final UdpBootstrap<T> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }
}
