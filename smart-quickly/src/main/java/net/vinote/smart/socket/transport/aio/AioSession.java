package net.vinote.smart.socket.transport.aio;

import net.vinote.smart.socket.service.filter.impl.SmartFilterChainImpl;
import net.vinote.smart.socket.transport.IoSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhengjunwei on 2017/6/29.
 */
public class AioSession<T> extends IoSession<T> {
    private Logger logger = LogManager.getLogger(AioSession.class);
    /**
     * 响应消息缓存队列
     */
    ArrayBlockingQueue<ByteBuffer> writeCacheQueue = new ArrayBlockingQueue(13000);

    public static final int RELEASE_LINE = 8000;

    public static final int FLOW_LIMIT_LINE = 10000;

    AsynchronousSocketChannel channel;

    private ReadCompletionHandler readCompletionHandler;
    private WriteCompletionHandler writeCompletionHandler;
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    AtomicBoolean flowLimit = new AtomicBoolean(false);

    boolean isServer;

    public AioSession(AsynchronousSocketChannel channel, IoServerConfig config) {
        super(ByteBuffer.allocate(config.getDataBufferSize()));
        this.channel = channel;
        super.protocol = config.getProtocolFactory().createProtocol();
        isServer = config.isServer();
        super.chain = new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters());
        readCompletionHandler = new ReadCompletionHandler(channel, this);
        writeCompletionHandler = new WriteCompletionHandler();
    }

    public void read(ByteBuffer readBuffer) {

        // 将从管道流中读取到的字节数据添加至当前会话中以便进行消息解析
        T dataEntry;
        while ((dataEntry = protocol.decode(readBuffer, this)) != null) {
            chain.doReadFilter(this, dataEntry);
        }
    }

    void registerReadHandler() {
        channel.read(readBuffer, readBuffer, readCompletionHandler);
    }

    @Override
    protected void close0() {

    }

    Semaphore semaphore = new Semaphore(1);

    @Override
    public void write(final ByteBuffer buffer) throws IOException {
        if (!isValid()) {
            return;
        }
//        chain.doWriteFilterStart(this, buffer);
        buffer.flip();
        try {
            writeCacheQueue.put(buffer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        if (!writeCacheQueue.pu(buffer)) {
//            logger.info("error");
//        }
        trigeWrite();
    }

    private void trigeWrite() {
        if (semaphore.tryAcquire()) {
            Iterator<ByteBuffer> iterable = writeCacheQueue.iterator();
            int totalSize = 0;
            while (iterable.hasNext()) {
                totalSize += iterable.next().remaining();
                if (totalSize >= 1024 * 1024) {
                    break;
                }
            }
            ByteBuffer buffer = ByteBuffer.allocate(totalSize);
            while (buffer.hasRemaining()) {
                buffer.put(writeCacheQueue.poll());
            }
            buffer.flip();
            channel.write(buffer, buffer, writeCompletionHandler);
        }
    }

    class WriteCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            if (isServer && writeCacheQueue.size() < RELEASE_LINE && flowLimit.get()) {
                flowLimit.set(false);
                registerReadHandler();
//                System.err.println("释放流控");
            }
            if (attachment.hasRemaining()) {
                channel.write(attachment, attachment, this);
                return;
            }
            if (writeCacheQueue.isEmpty()) {
                semaphore.release();
                if (!writeCacheQueue.isEmpty()) {
                    trigeWrite();
                }
            } else {
                Iterator<ByteBuffer> iterable = writeCacheQueue.iterator();
                int totalSize = 0;
                while (iterable.hasNext()) {
                    totalSize += iterable.next().remaining();
                    if (totalSize >= 1024 * 1024) {
                        break;
                    }
                }
                ByteBuffer buffer = ByteBuffer.allocate(totalSize);
                while (buffer.hasRemaining()) {
                    buffer.put(writeCacheQueue.poll());
                }
                buffer.flip();
                channel.write(buffer, buffer, writeCompletionHandler);
            }

        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            exc.printStackTrace();
        }
    }

}
