package org.smartboot.socket.transport;

import org.apache.commons.lang.builder.ToStringBuilder;
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
 * Created by seer on 2017/6/29.
 */
public class AioSession<T> {
    private static final Logger logger = LogManager.getLogger(AioSession.class);
    /**
     * Session ID生成器
     */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    /**
     * 唯一标识
     */
    private final int sessionId = NEXT_ID.getAndIncrement();

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
     * 会话当前状态
     */
    private volatile byte status = SESSION_STATUS_ENABLED;

    /**
     * 会话属性,延迟创建以减少内存消耗
     */
    private Map<String, Object> attribute;

    /**
     * 响应消息缓存队列
     */
    private ArrayBlockingQueue<ByteBuffer> writeCacheQueue;
    /**
     * 消息通信协议,用以消息编解码处理
     */
    private Protocol<T> protocol;

    /**
     * 消息过滤器,protocol解码成功的消息交由chain接收处理
     */
    private SmartFilterChain<T> chain;

    /**
     * 缓存传输层读取到的数据流
     */
    private ByteBuffer readBuffer;

    private ReadCompletionHandler<T> readCompletionHandler;
    private WriteCompletionHandler<T> writeCompletionHandler;

    private AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer> writeAttach = new AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer>(this, null);

    /**
     * 流控指标线
     */
    private final int flowLimitLine;

    /**
     * 释放流控指标线
     */
    private final int releaseLine;

    /**
     * 数据read限流标志,仅服务端需要进行限流
     */
    private AtomicBoolean serverFlowLimit;

    /**
     * 底层通信channel对象
     */
    AsynchronousSocketChannel channel;


    /**
     * 输出信号量
     */
    Semaphore semaphore = new Semaphore(1);


    public AioSession(AsynchronousSocketChannel channel, IoServerConfig<T> config, ReadCompletionHandler<T> readCompletionHandler, WriteCompletionHandler<T> writeCompletionHandler, SmartFilterChain<T> smartFilterChain) {
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
        flowLimitLine = (int) (config.getWriteQueueSize() * 0.9);
        releaseLine = (int) (config.getWriteQueueSize() * 0.6);
    }

    /**
     * 触发AIO的写操作
     */
    void writeToChannel() {
        if (isInvalid()) {
            close();
            logger.warn("end write because of aioSession's status is" + status);
            return;
        }
        //无法获得信号量则直接返回
        if (writeCacheQueue.isEmpty() || !semaphore.tryAcquire()) {
            return;
        }
        //对缓存中的数据进行压缩处理再输出
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
        writeAttach.setValue(buffer);
        channel.write(buffer, writeAttach, writeCompletionHandler);
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
                logger.debug("close connection:" + channel);
            } catch (IOException e) {
                logger.catching(e);
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


    @SuppressWarnings("unchecked")
    public final <T1> T1 getAttribute(String key) {
        return attribute == null ? null : (T1) attribute.get(key);
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
     * 当前会话是否已失效
     */
    public boolean isInvalid() {
        return status != SESSION_STATUS_ENABLED;
    }

    /**
     * 触发通道的读操作，当发现存在严重消息积压时,会触发流控
     */
    void readFromChannel() {
        readBuffer.flip();
        // 将从管道流中读取到的字节数据添加至当前会话中以便进行消息解析
        T dataEntry;
        int remain = 0;
        while ((remain = readBuffer.remaining()) > 0 && (dataEntry = protocol.decode(readBuffer, this)) != null) {
            chain.doChain(this, dataEntry, remain - readBuffer.remaining());
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

        //触发流控
        if (serverFlowLimit != null && writeCacheQueue.size() > flowLimitLine) {
            serverFlowLimit.set(true);
        }
        //正常读取
        else {
            channel.read(readBuffer, this, readCompletionHandler);
        }
    }

    public final void removeAttribute(String key) {
        if (attribute == null) {
            return;
        }
        attribute.remove(key);
    }


    public final void setAttribute(String key, Object value) {
        if (attribute == null) {
            attribute = new HashMap<String, Object>();
        }
        attribute.put(key, value);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


    /**
     * 如果存在流控并符合释放条件，则触发读操作
     */
    void tryReleaseFlowLimit() {
        if (serverFlowLimit != null && serverFlowLimit.get() && writeCacheQueue.size() < releaseLine) {
            serverFlowLimit.set(false);
            channel.read(readBuffer, this, readCompletionHandler);
        }
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
        writeToChannel();
    }

    public final void write(T t) throws IOException {
        write(protocol.encode(t, this));
    }
}
