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

    private AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer> writeAttach = new AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer>(this, null);
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
    private int RELEASE_LINE;

    /**
     * 流控指标线
     */
    private int FLOW_LIMIT_LINE;

    AsynchronousSocketChannel channel;

    private ReadCompletionHandler<T> readCompletionHandler;
    private WriteCompletionHandler<T> writeCompletionHandler;
    /**
     * 数据read限流标志,仅服务端需要进行限流
     */
    private AtomicBoolean serverFlowLimit;

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
        FLOW_LIMIT_LINE = (int) (config.getWriteQueueSize() * 0.9);
        RELEASE_LINE = (int) (config.getWriteQueueSize() * 0.6);
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
        channelReadProcess(false);
    }

    /**
     * Socket通道读操作
     * <p>该方法有一个入参releaseFlowLimitCheck用以校验是否释放流控</p>
     * <p>releaseFlowLimitCheck:false 校验当前输出缓冲区是否达到流控阈值，达到则设置流控标志serverFlowLimit.set(true),否则进行读操作</p>
     * <p>releaseFlowLimitCheck:true 校验当前输出缓冲区是否下降至释放流控阈值，达到则设置流控标志位serverFlowLimit.set(false)并触发读操作，否则不做任何处理</p>
     *
     * @param releaseFlowLimitCheck 是否触发释放流控校验
     */
    void channelReadProcess(boolean releaseFlowLimitCheck) {
        //释放流控,仅在WriteCompletionHandler中触发
        if (releaseFlowLimitCheck) {
            if (serverFlowLimit != null && writeCacheQueue.size() < RELEASE_LINE && serverFlowLimit.get()) {
                serverFlowLimit.set(false);
                channel.read(readBuffer, this, readCompletionHandler);
            }
            return;
        }

        //触发流控
        if (serverFlowLimit != null && writeCacheQueue.size() > FLOW_LIMIT_LINE) {
            serverFlowLimit.set(true);
        }
        //正常读取
        else {
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
        channelWriteProcess(true);
    }


    /**
     * 触发AIO的写操作
     *
     * @param ackSemaphore 是否申请信号量
     */
    void channelWriteProcess(boolean ackSemaphore) {
        if (isInvalid()) {
            logger.warn("AioSession channelWriteProcess is" + status);
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
            writeAttach.setValue(buffer);
            channel.write(buffer, writeAttach, writeCompletionHandler);
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
                logger.debug("close connection:" + channel);
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
