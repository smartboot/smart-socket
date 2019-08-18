package org.smartboot.socket.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.ring.EventFactory;
import org.smartboot.socket.buffer.ring.RingBuffer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Semaphore;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/16
 */
public final class UdpChannel<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpChannel.class);
    private DatagramChannel channel;
    private SelectionKey selectionKey;

    /**
     * 待输出消息
     */
    private RingBuffer<WriteEvent> writeRingBuffer;
    private IoServerConfig<T> config;
    /**
     * 已完成解码待业务处理的消息集合
     */
    private Object lock = new Object();

    private Semaphore writeSemaphore = new Semaphore(1);

    UdpChannel(final DatagramChannel channel, SelectionKey selectionKey, final IoServerConfig<T> config) {
        this.channel = channel;
        writeRingBuffer = new RingBuffer<>(2024, new EventFactory<WriteEvent>() {
            @Override
            public WriteEvent newInstance() {
                return new WriteEvent();
            }

            @Override
            public void restEntity(WriteEvent entity) {
                entity.setBuffer(null);
            }
        });
        this.selectionKey = selectionKey;
        this.config = config;
    }

    void doRead(ByteBuffer readBuffer, RingBuffer<ReadEvent<T>>[] readRingBuffers) throws IOException, InterruptedException {
//        LOGGER.info("doRead");
        SocketAddress remote = channel.receive(readBuffer);

        readBuffer.flip();
        //解码
        T t = config.getProtocol().decode(readBuffer);
        if (t == null) {
            System.out.println("decode null");
            return;
        }
        if (config.getThreadNum() == 0) {
            config.getProcessor().process(this, remote, t);
            return;
        }
        RingBuffer<ReadEvent<T>> ringBuffer = readRingBuffers[remote.hashCode() % config.getThreadNum()];
        int index = ringBuffer.nextWriteIndex();
        ReadEvent<T> udpEvent = ringBuffer.get(index);
        udpEvent.setRemote(remote);
        udpEvent.setMessage(t);
        udpEvent.setChannel(this);
        ringBuffer.publishWriteIndex(index);
        readBuffer.clear();
    }

    public void write(ByteBuffer byteBuffer, SocketAddress remote) throws InterruptedException, IOException {
        if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) == 0 && writeSemaphore.tryAcquire()) {
            channel.send(byteBuffer, remote);
            writeSemaphore.release();
//            System.out.println("aaa");
            return;
        }
//        System.out.println("bbb");
        int index = writeRingBuffer.nextWriteIndex();
        WriteEvent event = writeRingBuffer.get(index);
        event.setBuffer(byteBuffer);
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
            WriteEvent event = writeRingBuffer.get(index);
            ByteBuffer buffer = event.getBuffer();
            SocketAddress remote = event.getRemote();
            writeRingBuffer.publishReadIndex(index);
            writeSize = channel.send(buffer, remote);
            if (buffer.hasRemaining()) {
                LOGGER.error("buffer has remaining!");
            }
        } while (writeSize > 0);
    }

    void shutdown() {
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
