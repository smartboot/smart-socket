/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpBootstrap.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.util.DecoderException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
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
public class UdpBootstrap<Request> {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpBootstrap.class);

    private final static int MAX_EVENT = 512;

    private final static int MAX_READ_TIMES = 16;
    /**
     * 服务ID
     */
    private static int UID;
    private final SelectionKey NEED_TO_POLL = new UdpNullSelectionKey();
    private final SelectionKey EXECUTE_TASK_OR_SHUTDOWN = new UdpNullSelectionKey();
    /**
     * 缓存页
     */
    private final BufferPage bufferPage = new BufferPagePool(1024, 1, -1, true).allocateBufferPage();
    /**
     * 服务配置
     */
    private final IoServerConfig<Request> config = new IoServerConfig<>();
    /**
     * 服务状态
     */
    private volatile Status status = Status.STATUS_INIT;
    /**
     * 多路复用器
     */
    private Selector selector;
    private UdpDispatcher<Request>[] workerGroup;

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
        if (host != null) {
            config.setHost(host);
        }
        config.setPort(port);

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
            InetSocketAddress inetSocketAddress = host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
            channel.socket().bind(inetSocketAddress);
            if (host == null) {
                config.setHost(inetSocketAddress.getHostString());
            }
        } else {
            config.setHost("");
        }

        if (status == Status.STATUS_RUNNING) {
            selector.wakeup();
        }
        SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ);
        UdpChannel<Request> udpChannel = new UdpChannel<>(channel, selectionKey, config, bufferPage);
        selectionKey.attach(udpChannel);

        //启动线程服务
        if (status == Status.STATUS_INIT) {
            initThreadServer();
        }

        System.out.println("smart-socket server started on port " + config.getPort() + ",threadNum:" + config.getThreadNum());
        System.out.println("smart-socket server config is " + config);
        return udpChannel;
    }

    private synchronized void initThreadServer() {
        if (status != Status.STATUS_INIT) {
            return;
        }

        // 增加广告说明
        if (config.isBannerEnabled()) {
            System.out.println(IoServerConfig.BANNER + "\r\n :: smart-socket ::\t(" + IoServerConfig.VERSION + ")");
        }

        this.status = Status.STATUS_RUNNING;
        int uid = UdpBootstrap.UID++;

        //启动worker线程组
        workerGroup = new UdpDispatcher[config.getThreadNum()];
        for (int i = 0; i < config.getThreadNum(); i++) {
            workerGroup[i] = new UdpDispatcher<>(config.getProcessor());
            new Thread(workerGroup[i], "UDP-Worker-" + i).start();
        }
        //启动Boss线程组
        new Thread(new Runnable() {
            @Override
            public void run() {
                //读缓冲区
                VirtualBuffer readBuffer = bufferPage.allocate(config.getReadBufferSize());
                try {
                    while (true) {
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        if (selectionKeys.isEmpty()) {
                            selector.select();
                        }
                        Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            keyIterator.remove();
                            UdpChannel<Request> udpChannel = (UdpChannel<Request>) key.attachment();
                            if (!key.isValid()) {
                                udpChannel.close();
                                continue;
                            }

                            if (key.isReadable()) {
                                doRead(readBuffer, udpChannel);
                            }
                            if (key.isWritable()) {
                                udpChannel.flush();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    //读缓冲区内存回收
                    readBuffer.clean();
                }
            }
        }, "UDP-Boss-" + uid).start();
    }

    /**
     * 去读数据
     *
     * @param channel
     * @throws IOException
     */
    private void doRead(VirtualBuffer readBuffer, UdpChannel<Request> channel) throws IOException {
        int count = MAX_READ_TIMES;
        while (count-- > 0) {
            //接收数据
            ByteBuffer buffer = readBuffer.buffer();
            buffer.clear();
            //The datagram's source address,
            // or null if this channel is in non-blocking mode and no datagram was immediately available
            SocketAddress remote = channel.getChannel().receive(buffer);
            if (remote == null) {
                return;
            }
            buffer.flip();

            UdpAioSession aioSession = channel.createAndCacheSession(remote);
            NetMonitor netMonitor = config.getMonitor();
            if (netMonitor != null) {
                netMonitor.beforeRead(aioSession);
                netMonitor.afterRead(aioSession, buffer.remaining());
            }
            Request request;
            //解码
            try {
                request = config.getProtocol().decode(buffer, aioSession);
            } catch (Exception e) {
                config.getProcessor().stateEvent(aioSession, StateMachineEnum.DECODE_EXCEPTION, e);
                aioSession.close();
                throw e;
            }
            //理论上每个UDP包都是一个完整的消息
            if (request == null) {
                config.getProcessor().stateEvent(aioSession, StateMachineEnum.DECODE_EXCEPTION, new DecoderException("decode result is null"));
                return;
            }
//            LOGGER.info("receive:{} from:{}", request, remote);

            //任务分发
            int hashCode = remote.hashCode();
            if (hashCode < 0) {
                hashCode = -hashCode;
            }
            UdpDispatcher<Request> dispatcher = workerGroup[hashCode % workerGroup.length];
            dispatcher.dispatch(aioSession, request);
        }
    }

    public void shutdown() {
        status = Status.STATUS_STOPPING;
        selector.wakeup();

        for (UdpDispatcher<Request> dispatcher : workerGroup) {
            dispatcher.dispatch(dispatcher.EXECUTE_TASK_OR_SHUTDOWN);
        }
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


    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     * @return 当前AioQuickServer对象
     */
    public final UdpBootstrap<Request> setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    enum Status {
        /**
         * 状态：初始
         */
        STATUS_INIT,
        /**
         * 状态：初始
         */
        STATUS_STARTING,
        /**
         * 状态：运行中
         */
        STATUS_RUNNING,
        /**
         * 状态：停止中
         */
        STATUS_STOPPING,
        /**
         * 状态：已停止
         */
        STATUS_STOPPED;
    }
}

