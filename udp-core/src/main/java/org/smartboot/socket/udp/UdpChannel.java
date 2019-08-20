package org.smartboot.socket.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.pool.BufferPage;
import org.smartboot.socket.buffer.pool.VirtualBuffer;
import org.smartboot.socket.buffer.ring.EventFactory;
import org.smartboot.socket.buffer.ring.RingBuffer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
public final class UdpChannel<Request, Response> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpChannel.class);
    private DatagramChannel channel;
    private SelectionKey selectionKey;

    private BufferPage bufferPage;

    /**
     * 待输出消息
     */
    private RingBuffer<WriteEvent> writeRingBuffer;
    private IoServerConfig<Request, Response> config;
    /**
     * 已完成解码待业务处理的消息集合
     */
    private Object lock = new Object();

    UdpChannel(final DatagramChannel channel, SelectionKey selectionKey, final IoServerConfig<Request, Response> config, BufferPage bufferPage) {
        this.channel = channel;
        writeRingBuffer = new RingBuffer<>(2024, new EventFactory<WriteEvent>() {
            @Override
            public WriteEvent newInstance() {
                return new WriteEvent();
            }

            @Override
            public void restEntity(WriteEvent entity) {
                entity.setResponse(null);
                entity.setRemote(null);
            }
        });
        this.selectionKey = selectionKey;
        this.config = config;
        this.bufferPage = bufferPage;
    }

    void doRead(ByteBuffer persistReadBuffer, RingBuffer<ReadEvent<Request, Response>>[] readRingBuffers) throws IOException, InterruptedException {
        SocketAddress remote = channel.receive(persistReadBuffer);
        persistReadBuffer.flip();
        //解码
        Request t = config.getProtocol().decode(persistReadBuffer);
        if (t == null) {
            System.out.println("decode null");
            return;
        }
        if (config.getThreadNum() == 0) {
            config.getProcessor().process(this, remote, t);
            return;
        }
        RingBuffer<ReadEvent<Request, Response>> ringBuffer = readRingBuffers[remote.hashCode() % config.getThreadNum()];

        int index = -1;
        while ((index = ringBuffer.tryNextWriteIndex()) < 0) {
            //读缓冲区已满,尝试清空写缓冲区
            VirtualBuffer buffer = bufferPage.allocate(config.getWriteBufferSize());
            try {
                doWrite(buffer);
            } finally {
                buffer.clean();
            }
            //尝试消费一个读缓冲区资源
            int readIndex = ringBuffer.tryNextReadIndex();
            if (readIndex >= 0) {
                ReadEvent<Request, Response> event = ringBuffer.get(readIndex);
                SocketAddress address = event.getRemote();
                UdpChannel<Request, Response> readChannel = event.getChannel();
                Request message = event.getMessage();
                ringBuffer.publishReadIndex(readIndex);
                config.getProcessor().process(readChannel, address, message);
            }
        }
        ReadEvent<Request, Response> udpEvent = ringBuffer.get(index);
        udpEvent.setRemote(remote);
        udpEvent.setMessage(t);
        udpEvent.setChannel(this);
        ringBuffer.publishWriteIndex(index);
        persistReadBuffer.clear();
    }

    public void write(Response response, SocketAddress remote) throws IOException, InterruptedException {
        int index = writeRingBuffer.tryNextWriteIndex();
        //缓存区已满,同步输出
        if (index < 0) {
            VirtualBuffer virtualBuffer = bufferPage.allocate(config.getWriteBufferSize());
            config.getProtocol().encode(virtualBuffer.buffer(), response);
            try {
                channel.send(virtualBuffer.buffer(), remote);
            } finally {
                virtualBuffer.clean();
            }
            return;
        }
        WriteEvent<Response> event = writeRingBuffer.get(index);
        event.setResponse(response);
        event.setRemote(remote);
        writeRingBuffer.publishWriteIndex(index);

        if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
            synchronized (lock) {
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                selectionKey.selector().wakeup();
            }
        }
    }

    void doWrite(VirtualBuffer virtualBuffer) throws IOException {
//        LOGGER.info("doWrite");
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

            WriteEvent<Response> event = writeRingBuffer.get(index);
            Response response = event.getResponse();
            SocketAddress remote = event.getRemote();
            writeRingBuffer.publishReadIndex(index);

            ByteBuffer buffer = virtualBuffer.buffer();
            buffer.clear();
            config.getProtocol().encode(buffer, response);
            writeSize = channel.send(buffer, remote);

            if (buffer.hasRemaining()) {
                LOGGER.error("buffer has remaining!");
            }
        } while (writeSize > 0);
    }


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
    }

}
