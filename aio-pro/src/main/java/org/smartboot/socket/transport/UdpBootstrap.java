package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;

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
 * @param <Request>
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
public class UdpBootstrap<Request> implements Runnable {
    /**
     * logger
     */
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
    private static int UID;
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
    private IoServerConfig<Request> config = new IoServerConfig<>();

    /**
     * 已完成解码待业务处理的消息集合
     */
    private RingBuffer<UdpReadEvent<Request>>[] readRingBuffers;

    /**
     * 读缓冲区
     */
    private VirtualBuffer readBuffer;

    /**
     * EventFactory
     */
    private EventFactory<UdpReadEvent<Request>> factory = new EventFactory<UdpReadEvent<Request>>() {
        @Override
        public UdpReadEvent<Request> newInstance() {
            return new UdpReadEvent<>();
        }

        @Override
        public void restEntity(UdpReadEvent<Request> entity) {
            entity.setMessage(null);
            entity.setAioSession(null);
        }
    };

    /**
     * 缓存页
     */
    private BufferPage bufferPage = new BufferPagePool(1024, 1, true).allocateBufferPage();

    public UdpBootstrap(Protocol<Request> protocol, MessageProcessor<Request> messageProcessor) {
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    /**
     * 开启一个UDP通道，端口号随机
     *
     * @return UDP通道
     */
    public UdpChannel<Request> open() throws IOException {
        return open(0);
    }

    /**
     * 开启一个UDP通道
     *
     * @param port 指定绑定端口号,为0则随机指定
     */
    public UdpChannel<Request> open(int port) throws IOException {
        return open(null, port);
    }

    /**
     * 开启一个UDP通道
     *
     * @param host 绑定本机地址
     * @param port 指定绑定端口号,为0则随机指定
     */
    public UdpChannel<Request> open(String host, int port) throws IOException {
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
        UdpChannel<Request> udpChannel = new UdpChannel<>(channel, selectionKey, config, bufferPage);
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
            int uid = UdpBootstrap.UID++;
            Thread serverThread = new Thread(this, "UDP-Selector-" + uid);
            serverThread.start();

            readRingBuffers = new RingBuffer[config.getThreadNum()];
            for (int i = 0; i < config.getThreadNum(); i++) {
                final RingBuffer<UdpReadEvent<Request>> ringBuffer = readRingBuffers[i] = new RingBuffer<>(1024, factory);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (STATUS_RUNNING == status) {
                            try {
                                int index = ringBuffer.nextReadIndex();
                                if (STATUS_RUNNING != status) {
                                    break;
                                }
                                UdpReadEvent<Request> event = ringBuffer.get(index);
                                UdpAioSession<Request> aioSession = event.getAioSession();
                                Request message = event.getMessage();
                                ringBuffer.publishReadIndex(index);
                                config.getProcessor().process(aioSession, message);
                                aioSession.writeBuffer().flush();
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
        for (int i = 0; i < config.getThreadNum(); i++) {
            RingBuffer<UdpReadEvent<Request>> ringBuffer = readRingBuffers[i];
            try {
                int index = ringBuffer.tryNextWriteIndex();
                ringBuffer.publishWriteIndex(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
            UdpChannel<Request> udpChannel = (UdpChannel<Request>) key.attachment();
            try {
                if (!key.isValid()) {
                    udpChannel.close();
                    continue;
                }
                // 读取客户端数据
                if (key.isReadable()) {
                    doRead(udpChannel);
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
     * 去读数据
     *
     * @param channel
     * @throws IOException
     * @throws InterruptedException
     */
    private void doRead(UdpChannel channel) throws IOException, InterruptedException {
        //接收数据
        ByteBuffer buffer = readBuffer.buffer();
        SocketAddress remote = channel.getChannel().receive(buffer);
        buffer.flip();
        UdpAioSession<Request> aioSession = channel.createAndCacheSession(remote);

        //解码
        Request t = config.getProtocol().decode(buffer, aioSession);
        buffer.clear();
        if (t == null) {
            System.out.println("decode null");
            return;
        }

        if (config.getThreadNum() == 0) {
            //非异步则同步处理业务
            config.getProcessor().process(aioSession, t);
            aioSession.writeBuffer().flush();
            return;
        }
        int hashCode = remote.hashCode();
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        RingBuffer<UdpReadEvent<Request>> ringBuffer = readRingBuffers[hashCode % config.getThreadNum()];
        int index = -1;
        while ((index = ringBuffer.tryNextWriteIndex()) < 0) {
            //读缓冲区已满,尝试清空写缓冲区
            channel.doWrite();
            //尝试消费一个读缓冲区资源
            int readIndex = ringBuffer.tryNextReadIndex();
            if (readIndex >= 0) {
                UdpReadEvent<Request> event = ringBuffer.get(readIndex);
                UdpAioSession<Request> session = event.getAioSession();
                Request message = event.getMessage();
                ringBuffer.publishReadIndex(readIndex);
                config.getProcessor().process(session, message);
                aioSession.writeBuffer().flush();
            }
        }
        UdpReadEvent<Request> udpEvent = ringBuffer.get(index);
        udpEvent.setAioSession(aioSession);
        udpEvent.setMessage(t);
        ringBuffer.publishWriteIndex(index);
    }

    public void shutdown() {
        status = STATUS_STOPPING;
        selector.wakeup();
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final UdpBootstrap<Request> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }


    /**
     * 设置线程大小
     *
     * @param num
     */
    public final UdpBootstrap<Request> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }
}
