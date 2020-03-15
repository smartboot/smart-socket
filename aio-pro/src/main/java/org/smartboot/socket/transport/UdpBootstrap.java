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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

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
    /**
     * 服务ID
     */
    private static int UID;
    private final SelectionKey NEED_TO_POLL = new NullSelectionKey();
    private final SelectionKey EXECUTE_TASK_OR_SHUTDOWN = new NullSelectionKey();
    /**
     * 服务状态
     */
    private volatile Status status = Status.STATUS_INIT;
    /**
     * 多路复用器
     */
    private Selector selector;
    /**
     * 服务配置
     */
    private IoServerConfig<Request> config = new IoServerConfig<>();


    private ArrayBlockingQueue<SelectionKey> selectionKeys = new ArrayBlockingQueue<>(MAX_EVENT);

    private UdpDispatcher[] workerGroup;


    /**
     * 缓存页
     */
    private BufferPage bufferPage = new BufferPagePool(1024, 1, -1, true).allocateBufferPage();

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
        return udpChannel;
    }

    private synchronized void initThreadServer() {
        if (status != Status.STATUS_INIT) {
            return;
        }
        updateServiceStatus(Status.STATUS_RUNNING);
        int uid = UdpBootstrap.UID++;

        //启动worker线程组
        workerGroup = new UdpDispatcher[config.getThreadNum()];
        for (int i = 0; i < config.getThreadNum(); i++) {
            workerGroup[i] = new UdpDispatcher(config.getProcessor());
            new Thread(workerGroup[i], "UDP-Worker-" + i).start();
        }
        //启动Boss线程组
        selectionKeys.offer(NEED_TO_POLL);
        for (int i = 0; i < 2; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //读缓冲区
                    VirtualBuffer readBuffer = bufferPage.allocate(config.getReadBufferSize());
                    SelectionKey key;
                    try {
                        while (true) {
                            try {
                                key = selectionKeys.take();
                                if (key == NEED_TO_POLL) {
                                    try {
                                        key = poll();
                                    } catch (IOException x) {
                                        x.printStackTrace();
                                        return;
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                continue;
                            }
                            if (key == EXECUTE_TASK_OR_SHUTDOWN) {
                                LOGGER.info("stop thread");
                                break;
                            }

                            UdpChannel<Request> udpChannel = (UdpChannel<Request>) key.attachment();
                            if (!key.isValid()) {
                                udpChannel.close();
                                continue;
                            }

                            if (key.isReadable()) {
                                try {
                                    doRead(readBuffer, udpChannel);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else if (key.isWritable()) {
                                try {
                                    udpChannel.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } finally {
                        //读缓冲区内存回收
                        readBuffer.clean();
                    }
                }
            }, "UDP-Boss-" + uid + "-" + i).start();
        }
    }

    private void updateServiceStatus(final Status status) {
        this.status = status;
//        notifyWhenUpdateStatus(status);
    }

    /**
     * 获取待处理的Key
     *
     * @return
     * @throws IOException
     */
    private SelectionKey poll() throws IOException {
        try {
            while (true) {
                if (status != Status.STATUS_RUNNING) {
                    LOGGER.info("current status is :{}, will shutdown", status);
                    return EXECUTE_TASK_OR_SHUTDOWN;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                if (selectionKeys.isEmpty()) {
                    selector.select();
                }
                if (status != Status.STATUS_RUNNING) {
                    LOGGER.info("current status is :{}, will shutdown", status);
                    return EXECUTE_TASK_OR_SHUTDOWN;
                }
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                int max = selectionKeys.size();
                if (max > MAX_EVENT) {
                    max = MAX_EVENT;
                }

                while (max-- > 0) {
                    final SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    try {
                        if (max > 0) {
                            this.selectionKeys.offer(key);
                        } else {
                            return key;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            selectionKeys.offer(NEED_TO_POLL);
        }
    }

    /**
     * 去读数据
     *
     * @param channel
     * @throws IOException
     */
    private void doRead(VirtualBuffer readBuffer, UdpChannel channel) throws IOException {
        while (true) {
            //接收数据
            ByteBuffer buffer = readBuffer.buffer();
            buffer.clear();
            SocketAddress remote = channel.getChannel().receive(buffer);
            if (remote == null) {
                return;
            }
            buffer.flip();

            UdpAioSession<Request> aioSession = channel.createAndCacheSession(remote);
            config.getMonitor().beforeRead(aioSession);
            config.getMonitor().afterRead(aioSession, buffer.remaining());
            Request request = null;
            //解码
            try {
                request = config.getProtocol().decode(buffer, aioSession);
            } catch (Exception e) {
                config.getProcessor().stateEvent(aioSession, StateMachineEnum.DECODE_EXCEPTION, e);
                aioSession.close();
                throw e;
            }
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
            UdpDispatcher dispatcher = workerGroup[hashCode % workerGroup.length];
            dispatcher.dispatch(aioSession, request);
        }
    }

    public void shutdown() {
        status = Status.STATUS_STOPPING;
        selector.wakeup();

        for (UdpDispatcher dispatcher : workerGroup) {
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

    class NullSelectionKey extends SelectionKey {

        @Override
        public SelectableChannel channel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Selector selector() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValid() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int interestOps() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SelectionKey interestOps(int ops) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int readyOps() {
            throw new UnsupportedOperationException();
        }
    }
}

