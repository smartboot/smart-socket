package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.Protocol;
import org.smartboot.socket.service.filter.SmartFilterChain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AIO传输层会话
 * Created by zhengjunwei on 2017/6/29.
 */
public class AioSession<T> {
    private static final Logger logger = LogManager.getLogger(AioSession.class);
    /**
     * Session状态:已关闭
     */
    public static final byte SESSION_STATUS_CLOSED = 1;

    /**
     * Session状态:关闭中
     */
    public static final byte SESSION_STATUS_CLOSING = 2;

    /**
     * Session状态:正常
     */
    public static final byte SESSION_STATUS_ENABLED = 3;
    /**
     * Session ID生成器
     */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    /**
     * 唯一标识
     */
    private final int sessionId = NEXT_ID.getAndIncrement();

    /**
     * 消息通信协议
     */
    private Protocol<T> protocol;

    /**
     * 超时时间
     */
    private int timeout;

    /**
     * 缓存传输层读取到的数据流
     */
    private ByteBuffer readBuffer;

    /**
     * 会话属性,延迟创建以减少内存消耗
     */
    private Map<String, Object> attribute;

    /**
     * 会话状态
     */
    private volatile byte status = SESSION_STATUS_ENABLED;

    /**
     * 消息过滤器
     */
    private SmartFilterChain<T> chain;
    /**
     * 响应消息缓存队列
     */
    ArrayBlockingQueue<ByteBuffer> writeCacheQueue;

    /**
     * 输出信号量
     */
    Semaphore semaphore = new Semaphore(1);
    /**
     * 释放流控指标线
     */
    int RELEASE_LINE;

    /**
     * 流控指标线
     */
    int FLOW_LIMIT_LINE;

    AsynchronousSocketChannel channel;

    private ReadCompletionHandler readCompletionHandler;
    private WriteCompletionHandler<T> writeCompletionHandler;
    /**
     * 数据read限流标志,仅服务端需要进行限流
     */
    AtomicBoolean serverFlowLimit;

    public AioSession(AsynchronousSocketChannel channel, IoServerConfig config, ReadCompletionHandler readCompletionHandler, WriteCompletionHandler<T> writeCompletionHandler, SmartFilterChain smartFilterChain) {
        this.readBuffer = ByteBuffer.allocate(config.getReadBufferSize());
        this.channel = channel;
        this.protocol = config.getProtocol();
        if (config.isServer()) {
            serverFlowLimit = new AtomicBoolean(false);
        }
        this.chain = smartFilterChain;
        this.readCompletionHandler = readCompletionHandler;
        this.writeCompletionHandler = writeCompletionHandler;
        this.writeCacheQueue = new ArrayBlockingQueue<ByteBuffer>(config.getWriteQueueSize());
        FLOW_LIMIT_LINE = (int) (config.getWriteQueueSize() * 0.9);
        RELEASE_LINE = (int) (config.getWriteQueueSize() * 0.6);
        this.timeout = config.getTimeout();
    }


    void decodeAndProcess() {
        readBuffer.flip();
        // 将从管道流中读取到的字节数据添加至当前会话中以便进行消息解析
        T dataEntry;
        int remain = readBuffer.remaining();
        while ((dataEntry = protocol.decode(readBuffer, this)) != null) {
            chain.doChain(this, dataEntry, remain - readBuffer.remaining());
            remain = readBuffer.remaining();
        }
        //数据读取完毕
        if (readBuffer.remaining() == 0) {
            readBuffer.clear();
        } else if (readBuffer.position() > 0) {// 仅当发生数据读取时调用compact,减少内存拷贝
            readBuffer.compact();
        } else {
            readBuffer.position(readBuffer.limit());
            readBuffer.limit(readBuffer.capacity());
        }
    }

    void registerReadHandler() {
        channel.read(readBuffer, this, readCompletionHandler);
    }


    public void write(final ByteBuffer buffer) throws IOException {
        if (isInvalid()) {
            return;
        }
        buffer.flip();
        try {
            writeCacheQueue.put(buffer);
        } catch (InterruptedException e) {
            logger.error(e);
        }
        trigeWrite(true);
    }

    /**
     * 触发AIO的写操作
     *
     * @param ackSemaphore 是否申请信号量
     */
    void trigeWrite(boolean ackSemaphore) {
        if (isInvalid()) {
            logger.warn("AioSession trigeWrite status is" + status);
            return;
        }
        if (!ackSemaphore || semaphore.tryAcquire()) {
            //优先进行 自压缩：实测效果不理想
//            ByteBuffer firstBuffer = writeCacheQueue.peek();
//            if (firstBuffer != null && firstBuffer.capacity() - firstBuffer.limit() > firstBuffer.remaining()) {
//                firstBuffer = writeCacheQueue.poll();
//                if (firstBuffer.position() > 0) {
//                    firstBuffer.compact();
//                } else {
//                    firstBuffer.position(firstBuffer.limit());
//                    firstBuffer.limit(firstBuffer.capacity());
//                }
//                ByteBuffer nextBuffer;
//                while ((nextBuffer = writeCacheQueue.peek()) != null && firstBuffer.remaining() > nextBuffer.remaining()) {
//                    firstBuffer.put(writeCacheQueue.poll());
//                }
//                firstBuffer.flip();
//                channel.write(firstBuffer, new AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer>(this, firstBuffer), writeCompletionHandler);
//                return;
//            }

            Iterator<ByteBuffer> iterable = writeCacheQueue.iterator();
            int totalSize = 0;
            while (iterable.hasNext()) {
                totalSize += iterable.next().remaining();
                if (totalSize >= 32 * 1024) {
                    break;
                }
            }
            ByteBuffer buffer = ByteBuffer.allocate(totalSize);
            while (buffer.hasRemaining()) {
                buffer.put(writeCacheQueue.poll());
            }
            buffer.flip();
            channel.write(buffer, new AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer>(this, buffer), writeCompletionHandler);
        }
    }

    public final void close() {
        close(true);
    }

    /**
     * * 是否立即关闭会话
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    public void close(boolean immediate) {
        if (immediate) {
            try {
                channel.close();
            } catch (IOException e) {
                logger.debug(e);
            }
            status = SESSION_STATUS_CLOSED;
        } else {
            status = SESSION_STATUS_CLOSING;
            if (writeCacheQueue.isEmpty() && semaphore.tryAcquire()) {
                close(true);
                semaphore.release();
            }
        }
    }

    /**
     * 获取当前Session的唯一标识
     *
     * @return
     */
    public final int getSessionID() {
        return sessionId;
    }


    /**
     * 获取超时时间
     *
     * @return
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 当前会话是否已失效
     */
    public boolean isInvalid() {
        return status != SESSION_STATUS_ENABLED;
    }


    public final void write(T t) throws IOException {
        write(protocol.encode(t, this));
    }

    @SuppressWarnings("unchecked")
    public final <T1> T1 getAttribute(String key) {
        return attribute == null ? null : (T1) attribute.get(key);
    }


    public final void setAttribute(String key, Object value) {
        if (attribute == null) {
            attribute = new HashMap<String, Object>();
        }
        attribute.put(key, value);
    }

    public final void removeAttribute(String key) {
        if (attribute == null) {
            return;
        }
        attribute.remove(key);
    }
}
