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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 封装UDP底层真实渠道对象,并提供通信及会话管理
 *
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
public final class UdpChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpChannel.class);
    private final BufferPage writeBufferPage;

    /**
     * 待输出消息
     */
    private ConcurrentLinkedQueue<ResponseUnit> responseTasks;
    private Worker worker;
    final IoServerConfig config;
    /**
     * 真实的UDP通道
     */
    private final DatagramChannel channel;
    private SelectionKey selectionKey;
    //发送失败的
    private ResponseUnit failResponseUnit;

    UdpChannel(final DatagramChannel channel, IoServerConfig config, BufferPage writeBufferPage) {
        this.channel = channel;
        this.writeBufferPage = writeBufferPage;
        this.config = config;
    }

    UdpChannel(final DatagramChannel channel, Worker worker, IoServerConfig config, BufferPage writeBufferPage) {
        this(channel, config, writeBufferPage);
        responseTasks = new ConcurrentLinkedQueue<>();
        this.worker = worker;
        worker.addRegister(selector -> {
            try {
                UdpChannel.this.selectionKey = channel.register(selector, SelectionKey.OP_READ, UdpChannel.this);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        });
    }

    void write(VirtualBuffer virtualBuffer, UdpAioSession session) {
        if (send(virtualBuffer, session)) {
            return;
        }
        //已经持有write信号量，每个session在responseTasks中只会存一个带输出buffer
        responseTasks.offer(new ResponseUnit(session, virtualBuffer));
        synchronized (this) {
            if (selectionKey == null) {
                worker.addRegister(selector -> selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE));
            } else {
                if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                }
            }
        }
    }

    void doWrite() {
        while (true) {
            ResponseUnit responseUnit;
            if (failResponseUnit == null) {
                responseUnit = responseTasks.poll();
            } else {
                responseUnit = failResponseUnit;
                failResponseUnit = null;
            }
            if (responseUnit == null) {
                if (responseTasks.isEmpty()) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    if (!responseTasks.isEmpty()) {
                        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                    }
                }
                return;
            }
            if (!send(responseUnit.response, responseUnit.session)) {
                failResponseUnit = responseUnit;
                LOGGER.warn("send fail,will retry...");
                break;
            }
        }
    }

    private boolean send(VirtualBuffer virtualBuffer, UdpAioSession session) {
        if (config.getMonitor() != null) {
            config.getMonitor().beforeWrite(session);
        }
        int size = 0;
        try {
            size = channel.send(virtualBuffer.buffer(), session.getRemoteAddress());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (size == 0) {
            return false;
        }
        if (config.getMonitor() != null) {
            config.getMonitor().afterWrite(session, size);
        }
        virtualBuffer.clean();
        session.writeBuffer().finishWrite();
        session.writeBuffer().flush();
        return true;
    }

    /**
     * 建立与远程服务的连接会话,通过AioSession可进行数据传输
     */
    public AioSession connect(SocketAddress remote) {
        return new UdpAioSession(this, remote, writeBufferPage);
    }

    public AioSession connect(String host, int port) {
        return connect(new InetSocketAddress(host, port));
    }

    /**
     * 关闭当前连接
     */
    public void close() {
        LOGGER.info("close channel...");
        if (selectionKey != null) {
            Selector selector = selectionKey.selector();
            selectionKey.cancel();
            selector.wakeup();
            selectionKey = null;
        }
        try {
            if (channel != null) {
                channel.close();
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

    DatagramChannel getChannel() {
        return channel;
    }

    static final class ResponseUnit {
        /**
         * 待输出数据的接受地址
         */
        private final UdpAioSession session;
        /**
         * 待输出数据
         */
        private final VirtualBuffer response;

        public ResponseUnit(UdpAioSession session, VirtualBuffer response) {
            this.session = session;
            this.response = response;
        }

    }
}
