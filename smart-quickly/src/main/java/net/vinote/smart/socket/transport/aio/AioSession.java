package net.vinote.smart.socket.transport.aio;

import net.vinote.smart.socket.service.filter.impl.SmartFilterChainImpl;
import net.vinote.smart.socket.transport.IoSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhengjunwei on 2017/6/29.
 */
public class AioSession<T> extends IoSession<T> {
    private Logger logger = LogManager.getLogger(AioSession.class);
    /**
     * 响应消息缓存队列
     */
    LinkedBlockingQueue<ByteBuffer> writeCacheQueue = new LinkedBlockingQueue<ByteBuffer>();

    public static final int RELEASE_LINE = 8000;

    public static final int FLOW_LIMIT_LINE = 10000;

    AsynchronousSocketChannel channel;

    private ReadCompletionHandler readCompletionHandler;
    private WriteArrayCompletionHandler writeArrayCompletionHandler;
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    AtomicBoolean flowLimit = new AtomicBoolean(false);

    public AioSession(AsynchronousSocketChannel channel, IoServerConfig config) {
        super(null);
        this.channel = channel;
        super.protocol = config.getProtocolFactory().createProtocol();
        super.chain = new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters());
        readCompletionHandler = new ReadCompletionHandler(channel, this);
        writeArrayCompletionHandler = new WriteArrayCompletionHandler();
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
    Future<Integer> future;

    @Override
    public void write(final ByteBuffer buffer) throws IOException {
        if (!isValid()) {
            return;
        }
        chain.doWriteFilterStart(this, buffer);
        buffer.flip();
        if (!writeCacheQueue.offer(buffer)) {
            logger.info("error");
        }
        trigeWrite();
    }

    private void trigeWrite() {
        if (semaphore.tryAcquire()) {
            int size = writeCacheQueue.size();
            if (size > 1000) {
                size = 1000;
            }
            ByteBuffer[] array = new ByteBuffer[size];
            array = writeCacheQueue.toArray(array);
            channel.write(array, 0, array.length, 0, TimeUnit.MILLISECONDS, array, writeArrayCompletionHandler);
        }
    }

    /**
     * 获取写缓冲
     *
     * @return
     */
    public final ByteBuffer getWriteBuffer() {
        ByteBuffer buffer = null;
        // 移除已输出的数据流
        while ((buffer = writeCacheQueue.peek()) != null && buffer.remaining() == 0) {
            chain.doWriteFilterFinish(this, buffer);
            writeCacheQueue.remove(buffer);// 不要用poll,因为该行线程不安全
        }


        if (buffer != null) {
            chain.doWriteFilterContinue(this, buffer);
        }
        return buffer;
    }


    class WriteArrayCompletionHandler implements CompletionHandler<Long, ByteBuffer[]> {

        @Override
        public void completed(Long result, ByteBuffer[] attachment) {
            ByteBuffer buffer = getWriteBuffer();
            //释放流控
            if (writeCacheQueue.size() < RELEASE_LINE && flowLimit.get()) {
                flowLimit.set(false);
                registerReadHandler();
//                System.err.println("释放流控");
            }

            if (buffer == null) {
                semaphore.release();
                if (!writeCacheQueue.isEmpty()) {
                    trigeWrite();
                }
            } else {
                int size = writeCacheQueue.size();
                if (size > 1000) {
                    size = 1000;
                }
                ByteBuffer[] array = new ByteBuffer[size];
                array = writeCacheQueue.toArray(array);
                if (channel == null) {
                    System.err.println("channel is null");
                }
                if (array == null || array.length == 0) {
                    System.err.println(array);
                }
                channel.write(array, 0, array.length, 0, TimeUnit.MILLISECONDS, array, this);
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer[] attachment) {
            if (attachment != null) {
                logger.info(attachment.length);
                for (ByteBuffer b : attachment) {
                    logger.info(b);
                }
            }
            exc.printStackTrace();
        }
    }
}
