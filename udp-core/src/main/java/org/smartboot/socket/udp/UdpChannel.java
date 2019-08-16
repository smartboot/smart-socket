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

/**
 * @author 三刀
 * @version V1.0 , 2019/8/16
 */
public final class UdpChannel<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpChannel.class);
    private DatagramChannel channel;
    private SelectionKey selectionKey;
    private ByteBuffer readBuffer;
    private RingBuffer<WriteEvent> ringBuffer;
    private IoServerConfig<T> config;
    private RingBuffer<ReadEvent<T>>[] readBuffers;
    private Object lock = new Object();
    private EventFactory<ReadEvent<T>> factory = new EventFactory<ReadEvent<T>>() {
        @Override
        public ReadEvent<T> newInstance() {
            return new ReadEvent<>();
        }

        @Override
        public void restEntity(ReadEvent<T> entity) {
            entity.setMessage(null);
            entity.setRemote(null);
        }
    };

    UdpChannel(final DatagramChannel channel, SelectionKey selectionKey, final IoServerConfig<T> config) {
        this.channel = channel;
        ringBuffer = new RingBuffer<>(2024, new EventFactory<WriteEvent>() {
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
        readBuffer = ByteBuffer.allocate(config.getReadBufferSize());

        readBuffers = new RingBuffer[config.getThreadNum()];
        for (int i = 0; i < config.getThreadNum(); i++) {
            final RingBuffer<ReadEvent<T>> ringBuffer = readBuffers[i] = new RingBuffer<ReadEvent<T>>(1024, factory);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            int index = ringBuffer.nextReadIndex();
                            ReadEvent<T> event = ringBuffer.get(index);
                            SocketAddress remote = event.getRemote();
                            T message = event.getMessage();
                            ringBuffer.publishReadIndex(index);
                            config.getProcessor().process(UdpChannel.this, remote, message);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    void doRead() throws IOException, InterruptedException {
        SocketAddress remote = channel.receive(readBuffer);
        readBuffer.flip();
        //解码
        T t = config.getProtocol().decode(readBuffer);
        if (t == null) {
            System.out.println("decode null");
            return;
        }
        RingBuffer<ReadEvent<T>> ringBuffer = readBuffers[remote.hashCode() % config.getThreadNum()];
        int index = ringBuffer.nextWriteIndex();
        ReadEvent<T> udpEvent = ringBuffer.get(index);
        udpEvent.setRemote(remote);
        udpEvent.setMessage(t);
        ringBuffer.publishWriteIndex(index);
        readBuffer.clear();
    }

    public void write(ByteBuffer byteBuffer, SocketAddress remote) throws InterruptedException {
        int index = ringBuffer.nextWriteIndex();
        WriteEvent event = ringBuffer.get(index);
        event.setBuffer(byteBuffer);
        event.setRemote(remote);
        ringBuffer.publishWriteIndex(index);

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
            int index = ringBuffer.tryNextReadIndex();
            //无可写数据,去除写关注
            if (index < 0) {
                synchronized (lock) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    selectionKey.selector().wakeup();
                }
                index = ringBuffer.tryNextReadIndex();
                if (index < 0) {
                    return;
                } else {
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                }
            }
            WriteEvent event = ringBuffer.get(index);
            ByteBuffer buffer = event.getBuffer();
            SocketAddress remote = event.getRemote();
            ringBuffer.publishReadIndex(index);
            writeSize = channel.send(buffer, remote);
            if (buffer.hasRemaining()) {
                LOGGER.error("buffer has remaining!");
            }
        } while (writeSize > 0);
    }
}
