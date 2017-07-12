package net.vinote.smart.socket.transport.aio;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.service.filter.impl.SmartFilterChainImpl;
import net.vinote.smart.socket.transport.TransportSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhengjunwei on 2017/6/29.
 */
public class AioSession<T> extends TransportSession<T> {
    private Logger logger = LogManager.getLogger(AioSession.class);
    /**
     * 响应消息缓存队列
     */
    private ArrayBlockingQueue<ByteBuffer> writeCacheQueue;
    private AsynchronousSocketChannel channel;
    static ThreadLocal<SessionWriteThread> writeThread = new ThreadLocal<SessionWriteThread>() {
        @Override
        protected SessionWriteThread initialValue() {
            SessionWriteThread thread = new SessionWriteThread();
            thread.setName("SessionWriteThread-" + System.currentTimeMillis());
            thread.start();
            return thread;
        }
    };

    public AioSession(AsynchronousSocketChannel channel, QuicklyConfig config) {
        super(ByteBuffer.allocate(config.getDataBufferSize()));
        this.channel = channel;
        super.protocol = config.getProtocolFactory().createProtocol();
        super.chain = new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters());
        super.cacheSize = config.getCacheSize();
        writeCacheQueue = new ArrayBlockingQueue<ByteBuffer>(cacheSize);
        super.bufferSize = config.getDataBufferSize();
        super.timeout = config.getTimeout();
    }

    private AtomicBoolean writing = new AtomicBoolean(false);

    @Override
    protected void cancelReadAttention() {

    }

    @Override
    protected void close0() {

    }

    @Override
    public String getLocalAddress() {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public void pauseReadAttention() {

    }

    @Override
    public void resumeReadAttention() {

    }

    @Override
    public void write(final ByteBuffer buffer) throws IOException {
        if (!isValid()) {
            return;
        }
        chain.doWriteFilterStart(this, buffer);
        buffer.flip();
        int size = writeCacheQueue.size();
        boolean suc = writeCacheQueue.offer(buffer);
        if (size == 0) {
            writeThread.get().notifySession(this);
        }
        if (!suc) {
            try {
                writeCacheQueue.put(buffer);
//                writeThread.get().notifySession(this);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            }
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


        if (buffer == null) {
            resumeReadAttention();// 此前若触发过流控,则在消息发送完毕后恢复读关注
        } else {
            chain.doWriteFilterContinue(this, buffer);
        }
        return buffer;
    }

    synchronized void flushWriteBuffer() throws IOException {
        ByteBuffer[] array = new ByteBuffer[cacheSize];
        Iterator<ByteBuffer> iterable = writeCacheQueue.iterator();
        int i = 0;
        while (iterable.hasNext()) {
            array[i++] = iterable.next();
            if (i >= cacheSize) {
                break;
            }
        }
        channel.write(array, 0, i, 1, TimeUnit.SECONDS, null, new CompletionHandler<Long, Object>() {
            @Override
            public void completed(Long result, Object attachment) {

                ByteBuffer byteBuffer = getWriteBuffer();
                if (byteBuffer != null) {
                    System.out.println("notify");
                    writeThread.get().notifySession(AioSession.this);//未输出完,再次唤醒
                }

            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        });

    }
}
