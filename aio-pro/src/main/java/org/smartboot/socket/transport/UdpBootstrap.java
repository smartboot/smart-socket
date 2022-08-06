/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpBootstrap.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * UDP服务启动类
 *
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
public class UdpBootstrap {
    private static final UdpChannel SELECTOR_CHANNEL = new UdpChannel();
    private static final UdpChannel SHUTDOWN_CHANNEL = new UdpChannel();

    private final static int MAX_READ_TIMES = 16;
    /**
     * 服务ID
     */
    private static int UID;
    /**
     * 缓存页
     */
    private final BufferPage bufferPage = new BufferPagePool(1024 * 1024, 1, true).allocateBufferPage();
    /**
     * 服务配置
     */
    private final IoServerConfig config = new IoServerConfig();

    private Worker worker;

    private ExecutorService executorService;

    public <Request> UdpBootstrap(Protocol<Request> protocol, MessageProcessor<Request> messageProcessor) {
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    /**
     * 开启一个UDP通道，端口号随机
     *
     * @return UDP通道
     */
    public UdpChannel open() throws IOException {
        return open(0);
    }

    /**
     * 开启一个UDP通道
     *
     * @param port 指定绑定端口号,为0则随机指定
     */
    public UdpChannel open(int port) throws IOException {
        return open(null, port);
    }

    /**
     * 开启一个UDP通道
     *
     * @param host 绑定本机地址
     * @param port 指定绑定端口号,为0则随机指定
     */
    public UdpChannel open(String host, int port) throws IOException {
        //启动线程服务
        if (worker == null) {
            initWorker();
        }

        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        if (port > 0) {
            InetSocketAddress inetSocketAddress = host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
            channel.socket().bind(inetSocketAddress);
        }
        return new UdpChannel(channel, worker, config, bufferPage);
    }

    private synchronized void initWorker() throws IOException {
        if (worker != null) {
            return;
        }

        // 增加广告说明
        if (config.isBannerEnabled()) {
            System.out.println(IoServerConfig.BANNER + "\r\n :: smart-socket[udp] ::\t(" + IoServerConfig.VERSION + ")");
        }

        int uid = UdpBootstrap.UID++;

        //启动worker线程组
        executorService = new ThreadPoolExecutor(config.getThreadNum(), config.getThreadNum(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new ThreadFactory() {
            int i = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "smart-socket:udp-" + uid + "-" + (++i));
            }
        });

        worker = new Worker();
        for (int i = 0; i < config.getThreadNum(); i++) {
            executorService.execute(worker);
        }
    }

    private void doRead(ByteBuffer buffer, UdpChannel channel) throws IOException {
        int count = MAX_READ_TIMES;
        while (count-- > 0) {
            //接收数据
            buffer.clear();
            SocketAddress remote = channel.getChannel().receive(buffer);
            if (remote == null) {
                return;
            }
            buffer.flip();

            UdpAioSession session = new UdpAioSession(channel, remote, bufferPage);
            NetMonitor netMonitor = config.getMonitor();
            if (netMonitor != null) {
                netMonitor.beforeRead(session);
                netMonitor.afterRead(session, buffer.remaining());
            }
            //解码
            try {
                while (buffer.hasRemaining()) {
                    Object request = config.getProtocol().decode(buffer, session);
                    //理论上每个UDP包都是一个完整的消息
                    if (request == null) {
                        config.getProcessor().stateEvent(session, StateMachineEnum.DECODE_EXCEPTION, new DecoderException("decode result is null"));
                        break;
                    } else {
                        config.getProcessor().process(session, request);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                config.getProcessor().stateEvent(session, StateMachineEnum.DECODE_EXCEPTION, e);
                throw e;
            } finally {
                session.close();
            }
        }
    }

    public void shutdown() {
        System.out.println("shutdown...");
        worker.selectionKeys.offer(SHUTDOWN_CHANNEL);
        worker.selector.wakeup();
        executorService.shutdown();
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final UdpBootstrap setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }


    /**
     * 设置线程大小
     *
     * @param num 线程数
     */
    public final UdpBootstrap setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }


    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     * @return 当前AioQuickServer对象
     */
    public final UdpBootstrap setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    class Worker implements Runnable {
        /**
         * 当前Worker绑定的Selector
         */
        private final Selector selector;
        private final BlockingQueue<UdpChannel> selectionKeys = new ArrayBlockingQueue<>(256);

        /**
         * 待注册的事件
         */
        private final ConcurrentLinkedQueue<Consumer<Selector>> registers = new ConcurrentLinkedQueue<>();

        Worker() throws IOException {
            this.selector = Selector.open();
            try {
                this.selectionKeys.put(SELECTOR_CHANNEL);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 注册事件
         */
        final void addRegister(Consumer<Selector> register) {
            registers.offer(register);
            selector.wakeup();
        }

        @Override
        public final void run() {
            //读缓冲区
            VirtualBuffer readBuffer = bufferPage.allocate(config.getReadBufferSize());
            try {
                while (true) {
                    UdpChannel udpChannel = selectionKeys.take();
                    if (udpChannel == SELECTOR_CHANNEL) {
                        try {
                            doSelect();
                        } finally {
                            selectionKeys.put(SELECTOR_CHANNEL);
                        }
                        continue;
                    } else if (udpChannel == SHUTDOWN_CHANNEL) {
                        selectionKeys.put(SHUTDOWN_CHANNEL);
                        selector.wakeup();
                        break;
                    }
                    SelectionKey key = udpChannel.getSelectionKey();
                    if (!key.isValid()) {
                        udpChannel.close();
                        continue;
                    }

                    if (key.isReadable()) {
                        doRead(readBuffer.buffer(), udpChannel);
                    }
                    if (key.isWritable()) {
                        System.out.println("writing...");
                        udpChannel.doWrite();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //读缓冲区内存回收
                readBuffer.clean();
            }
        }

        private void doSelect() throws IOException {
            Consumer<Selector> register;
            while ((register = registers.poll()) != null) {
                register.accept(selector);
            }
            Set<SelectionKey> keySet = selector.selectedKeys();
            if (keySet.isEmpty()) {
                selector.select();
            }
            Iterator<SelectionKey> keyIterator = keySet.iterator();
            // 执行本次已触发待处理的事件
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (selectionKeys.offer((UdpChannel) key.attachment())) {
                    keyIterator.remove();
                } else {
                    break;
                }
            }
        }
    }
}

