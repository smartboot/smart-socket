package org.smartboot.socket.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.pool.BufferPage;
import org.smartboot.socket.buffer.pool.BufferPagePool;
import org.smartboot.socket.buffer.pool.VirtualBuffer;
import org.smartboot.socket.buffer.ring.EventFactory;
import org.smartboot.socket.buffer.ring.RingBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
 * @version V1.0 , 2019/8/18
 */
public class UdpBootstrap<Request, Response> implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpBootstrap.class);
    /**
     * 状态：初始
     */
    private static final byte STATUS_INIT = 0;
    /**
     * 状态：初始
     */
    private static final byte STATUS_STARTING = 1;
    /**
     * 状态：运行中
     */
    private static final byte STATUS_RUNNING = STATUS_STARTING << 1;
    /**
     * 状态：停止中
     */
    private static final byte STATUS_STOPPING = STATUS_RUNNING << 1;
    /**
     * 状态：已停止
     */
    private static final byte STATUS_STOPPED = STATUS_STOPPING << 1;
    /**
     * 服务ID
     */
    private static int uid;
    /**
     * 服务状态
     */
    private volatile byte status = STATUS_INIT;
    /**
     * 多路复用器
     */
    private Selector selector;

    /**
     * 服务配置
     */
    private IoServerConfig<Request, Response> config = new IoServerConfig<>();

    /**
     * 已完成解码待业务处理的消息集合
     */
    private RingBuffer<ReadEvent<Request, Response>>[] readRingBuffers;

    /**
     * 读缓冲区
     */
    private VirtualBuffer readBuffer;

    /**
     * 写缓冲区
     */
    private VirtualBuffer writeBuffer;

    private EventFactory<ReadEvent<Request, Response>> factory = new EventFactory<ReadEvent<Request, Response>>() {
        @Override
        public ReadEvent<Request, Response> newInstance() {
            return new ReadEvent<>();
        }

        @Override
        public void restEntity(ReadEvent<Request, Response> entity) {
            entity.setMessage(null);
            entity.setRemote(null);
            entity.setChannel(null);
        }
    };

    private BufferPage bufferPage = new BufferPagePool(1024, 1, true).allocateBufferPage();

    public UdpBootstrap(Protocol<Request, Response> protocol, MessageProcessor<Request, Response> messageProcessor) {
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    /**
     * 开启一个UDP通道，端口号随机
     *
     * @return UDP通道
     */
    public UdpChannel<Request, Response> open() throws IOException {
        return open(0);
    }

    /**
     * 开启一个UDP通道
     *
     * @param port 指定绑定端口号,为0则随机指定
     */
    public UdpChannel<Request, Response> open(int port) throws IOException {
        return open(null, port);
    }


    /**
     * 开启一个UDP通道
     *
     * @param host 绑定本机地址
     * @param port 指定绑定端口号,为0则随机指定
     */
    public UdpChannel<Request, Response> open(String host, int port) throws IOException {
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
            channel.socket().bind(host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port));
        }

        if (status == STATUS_RUNNING) {
            selector.wakeup();
        }
        SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ);
        UdpChannel<Request, Response> udpChannel = new UdpChannel<>(channel, selectionKey, config, bufferPage);
        selectionKey.attach(udpChannel);

        //启动线程服务
        initThreadServer();
        return udpChannel;
    }

    private void initThreadServer() {
        if (status != STATUS_INIT) {
            return;
        }
        synchronized (this) {
            if (status != STATUS_INIT) {
                return;
            }
            updateServiceStatus(STATUS_STARTING);

            readBuffer = bufferPage.allocate(config.getReadBufferSize());
            writeBuffer = bufferPage.allocate(config.getWriteBufferSize());
            int uid = UdpBootstrap.uid++;
            Thread serverThread = new Thread(this, "UDP-Selector-" + uid);
            serverThread.start();

            readRingBuffers = new RingBuffer[config.getThreadNum()];
            for (int i = 0; i < config.getThreadNum(); i++) {
                final RingBuffer<ReadEvent<Request, Response>> ringBuffer = readRingBuffers[i] = new RingBuffer<>(1024, factory);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (STATUS_RUNNING == status) {
                            try {
                                int index = ringBuffer.nextReadIndex();
                                if (STATUS_RUNNING != status) {
                                    break;
                                }
                                ReadEvent<Request, Response> event = ringBuffer.get(index);
                                SocketAddress remote = event.getRemote();
                                UdpChannel<Request, Response> channel = event.getChannel();
                                Request message = event.getMessage();
                                ringBuffer.publishReadIndex(index);
                                config.getProcessor().process(channel, remote, message);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, "UDP-Worker-" + uid + "-" + i).start();
            }
        }
    }

    private void updateServiceStatus(final byte status) {
        this.status = status;
//        notifyWhenUpdateStatus(status);
    }

    @Override
    public void run() {
        updateServiceStatus(STATUS_RUNNING);
        // 通过检查状态使之一直保持服务状态
        while (STATUS_RUNNING == status) {
            try {
                running();
            } catch (ClosedSelectorException e) {
                e.printStackTrace();// Selector关闭触发服务终止
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            selector = null;
        }
        updateServiceStatus(STATUS_STOPPED);
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
            UdpChannel<Request, Response> udpChannel = (UdpChannel<Request, Response>) key.attachment();
            try {
                if (!key.isValid()) {
                    udpChannel.close();
                    continue;
                }
                // 读取客户端数据
                if (key.isReadable()) {
                    udpChannel.doRead(readBuffer.buffer(), readRingBuffers);
                } else if (key.isWritable()) {
                    udpChannel.doWrite(writeBuffer);
                } else {
                    LOGGER.warn("奇怪了...");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        selectionKeys.clear();
    }

    public void shutdown() {
        status = STATUS_STOPPING;

        for (int i = 0; i < config.getThreadNum(); i++) {
            RingBuffer<ReadEvent<Request, Response>> ringBuffer = readRingBuffers[i];
            try {
                int index = ringBuffer.tryNextWriteIndex();
                ringBuffer.publishWriteIndex(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final UdpBootstrap<Request, Response> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }


    /**
     * 设置线程大小
     *
     * @param num
     */
    public final UdpBootstrap<Request, Response> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }
}
