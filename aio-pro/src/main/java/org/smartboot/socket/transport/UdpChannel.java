package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.VirtualBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
public final class UdpChannel<Request> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpChannel.class);
    private BufferPage bufferPage;
    private IoServerConfig config;
    /**
     * 真实的UDP通道
     */
    private DatagramChannel channel;

    private SelectionKey selectionKey;

    /**
     * 与当前UDP通道对接的会话
     */
    private ConcurrentHashMap<String, UdpAioSession<Request>> udpAioSessionConcurrentHashMap = new ConcurrentHashMap<>();


    /**
     * 待输出消息
     */
    private RingBuffer<UdpWriteEvent> writeRingBuffer;
    /**
     * 已完成解码待业务处理的消息集合
     */
    private Object lock = new Object();

    /**
     *
     */
    private int writeBacklog = 2048;

    UdpChannel(final DatagramChannel channel, SelectionKey selectionKey, IoServerConfig config, BufferPage bufferPage) {
        this.channel = channel;
        writeRingBuffer = new RingBuffer<>(writeBacklog, new EventFactory<UdpWriteEvent>() {
            @Override
            public UdpWriteEvent newInstance() {
                return new UdpWriteEvent();
            }

            @Override
            public void restEntity(UdpWriteEvent entity) {
                entity.setResponse(null);
                entity.setRemote(null);
            }
        });
        this.selectionKey = selectionKey;
        this.config = config;
        this.bufferPage = bufferPage;
    }

    /**
     * @param virtualBuffer
     * @param remote
     * @throws IOException
     * @throws InterruptedException
     */
    private void write(VirtualBuffer virtualBuffer, SocketAddress remote) throws IOException, InterruptedException {
        int index = writeRingBuffer == null ? -1 : writeRingBuffer.tryNextWriteIndex();
        //缓存区已满,同步输出确保线程不发送死锁
        if (index < 0) {
            try {
                channel.send(virtualBuffer.buffer(), remote);
            } finally {
                virtualBuffer.clean();
            }
            return;
        }
        UdpWriteEvent event = writeRingBuffer.get(index);
        event.setResponse(virtualBuffer);
        event.setRemote(remote);
        writeRingBuffer.publishWriteIndex(index);

        if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
            synchronized (lock) {
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                selectionKey.selector().wakeup();
            }
        }
    }

    void doWrite() throws IOException {
        int writeSize = -1;
        do {
            int index = writeRingBuffer.tryNextReadIndex();
            //无可写数据,去除写关注
            if (index < 0) {
                synchronized (lock) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    selectionKey.selector().wakeup();
                }
                index = writeRingBuffer.tryNextReadIndex();
                if (index < 0) {
                    return;
                } else {
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                }
            }

            UdpWriteEvent event = writeRingBuffer.get(index);
            VirtualBuffer response = event.getResponse();
            SocketAddress remote = event.getRemote();
            writeRingBuffer.publishReadIndex(index);

            ByteBuffer buffer = response.buffer();
            writeSize = channel.send(buffer, remote);
            response.clean();
            if (buffer.hasRemaining()) {
                LOGGER.error("buffer has remaining!");
            }
        } while (writeSize > 0);
    }

    /**
     * 建立与远程服务的连接会话,通过AioSession可进行数据传输
     *
     * @param remote
     * @return
     */
    public AioSession<Request> connect(SocketAddress remote) {
        return createAndCacheSession(remote);
    }

    /**
     * 创建并缓存与指定地址的会话信息
     *
     * @param remote
     * @return
     */
    UdpAioSession<Request> createAndCacheSession(final SocketAddress remote) {
        if (!(remote instanceof InetSocketAddress)) {
            throw new UnsupportedOperationException();

        }
        InetSocketAddress address = (InetSocketAddress) remote;
        String key = address.getHostName() + ":" + address.getPort();
        UdpAioSession<Request> session = udpAioSessionConcurrentHashMap.get(key);
        if (session != null) {
            return session;
        }
        synchronized (this) {
            if (session != null) {
                return session;
            }
            Function<WriteBuffer, Void> function = new Function<WriteBuffer, Void>() {
                @Override
                public Void apply(WriteBuffer writeBuffer) {
                    VirtualBuffer virtualBuffer = writeBuffer.poll();
                    if (virtualBuffer == null) {
                        return null;
                    }
                    try {
                        write(virtualBuffer, remote);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            WriteBuffer writeBuffer = new WriteBuffer(bufferPage, function, config);
            session = new UdpAioSession<>(this, remote, writeBuffer);
            udpAioSessionConcurrentHashMap.put(key, session);
        }
        return session;
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
        for (Map.Entry<String, UdpAioSession<Request>> entry : udpAioSessionConcurrentHashMap.entrySet()) {
            entry.getValue().close();
        }
        try {
            if (channel != null) {
                channel.close();
                channel = null;
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    DatagramChannel getChannel() {
        return channel;
    }
}
