/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpChannel.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.VirtualBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * 封装UDP底层真实渠道对象,并提供通信及会话管理
 *
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
public final class UdpChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpChannel.class);
    private BufferPage bufferPage;

    /**
     * 待输出消息
     */
    private ConcurrentLinkedQueue<ResponseUnit> responseTasks;
    private final Semaphore writeSemaphore = new Semaphore(1);
    private UdpBootstrap.Worker worker;
    IoServerConfig config;
    /**
     * 真实的UDP通道
     */
    private DatagramChannel channel;
    private SelectionKey selectionKey;
    private ResponseUnit failResponseUnit;

    public UdpChannel() {
    }

    UdpChannel(final DatagramChannel channel, UdpBootstrap.Worker worker, IoServerConfig config, BufferPage bufferPage) {
        this.channel = channel;
        responseTasks = new ConcurrentLinkedQueue<>();
        this.worker = worker;
        this.bufferPage = bufferPage;
        this.config = config;
        worker.addRegister(selector -> {
            try {
                SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ, UdpChannel.this);
                setSelectionKey(selectionKey);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        });
    }

    void write(VirtualBuffer virtualBuffer, UdpAioSession remote) throws IOException {
        if (writeSemaphore.tryAcquire() && responseTasks.isEmpty() && send(virtualBuffer.buffer(), remote) > 0) {
            virtualBuffer.clean();
            writeSemaphore.release();
            return;
        }
        responseTasks.offer(new ResponseUnit(remote, virtualBuffer));
        if (selectionKey == null) {
            worker.addRegister(selector -> selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE));
        } else {
            if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
            }
        }
    }

    void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    void doWrite() throws IOException {
        while (true) {
            ResponseUnit responseUnit;
            if (failResponseUnit == null) {
                responseUnit = responseTasks.poll();
                LOGGER.info("poll from writeBuffer");
            } else {
                responseUnit = failResponseUnit;
                failResponseUnit = null;
            }
            if (responseUnit == null) {
                writeSemaphore.release();
                if (responseTasks.isEmpty()) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    if (!responseTasks.isEmpty()) {
                        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                    }
                }
                return;
            }
            if (send(responseUnit.response.buffer(), responseUnit.remote) > 0) {
                responseUnit.response.clean();
            } else {
                failResponseUnit = responseUnit;
                break;
            }
        }
    }

    private int send(ByteBuffer byteBuffer, UdpAioSession session) throws IOException {
        if (config.getMonitor() != null) {
            config.getMonitor().beforeWrite(session);
        }
        int size = channel.send(byteBuffer, session.getRemoteAddress());
        if (config.getMonitor() != null) {
            config.getMonitor().afterWrite(session, size);
        }
        return size;
    }

    /**
     * 建立与远程服务的连接会话,通过AioSession可进行数据传输
     */
    public AioSession connect(SocketAddress remote) {
        return new UdpAioSession(this, remote, bufferPage);
    }

    public AioSession connect(String host, int port) {
        return connect(new InetSocketAddress(host, port));
    }

    /**
     * 关闭当前连接
     */
    public void close() {
        if (selectionKey != null) {
            Selector selector = selectionKey.selector();
            selectionKey.cancel();
            selector.wakeup();
            selectionKey = null;
        }
        try {
            if (channel != null) {
                channel.close();
                channel = null;
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        }
        //内存回收
        ResponseUnit task;
        while ((task = responseTasks.poll()) != null) {
            task.response.clean();
        }
        if (failResponseUnit != null) {
            failResponseUnit.response.clean();
        }
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    DatagramChannel getChannel() {
        return channel;
    }

    static final class ResponseUnit {
        /**
         * 待输出数据的接受地址
         */
        private final UdpAioSession remote;
        /**
         * 待输出数据
         */
        private final VirtualBuffer response;

        public ResponseUnit(UdpAioSession remote, VirtualBuffer response) {
            this.remote = remote;
            this.response = response;
        }

    }
}
