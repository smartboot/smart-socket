package org.smartboot.socket.transport;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.service.SmartFilter;
import org.smartboot.socket.util.StateMachineEnum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * AIO传输层会话
 * Created by 三刀 on 2017/6/29.
 */
public class AioSession<T> {
    private static final Logger logger = LogManager.getLogger(AioSession.class);

    /* Session状态:已关闭 */
    private static final byte SESSION_STATUS_CLOSED = 1,
    /*Session状态:关闭中*/
    SESSION_STATUS_CLOSING = 2,
    /* Session状态:正常 */
    SESSION_STATUS_ENABLED = 3;

    /* Session ID生成器 */
    private static int NEXT_ID = 0;

    /**
     * 唯一标识
     */
    private final int sessionId = ++NEXT_ID;
    Future<Integer> future;
    /**
     * 会话当前状态
     */
    private byte status = SESSION_STATUS_ENABLED;
    /**
     * 附件对象
     */
    private Object attachment;
    /**
     * 响应消息缓存队列
     */
    private ArrayBlockingQueue<ByteBuffer> writeCacheQueue;
    /**
     * 数据read限流标志,仅服务端需要进行限流
     */
    private Boolean serverFlowLimit;
    /**
     * Channel读写操作回调Handler
     */
    private AioCompletionHandler aioCompletionHandler;
    /**
     * 读写回调附件
     */
    private Attachment readAttach = new Attachment(true), writeAttach = new Attachment(false);
    /**
     * 底层通信channel对象
     */
    private AsynchronousSocketChannel channel;
    /**
     * 输出信号量
     */
    private Semaphore semaphore = new Semaphore(1);
    private IoServerConfig<T> ioServerConfig;

    /**
     * @param channel
     * @param config
     * @param aioCompletionHandler
     * @param serverSession        是否服务端Session
     */
    AioSession(AsynchronousSocketChannel channel, IoServerConfig<T> config, AioCompletionHandler aioCompletionHandler, boolean serverSession) {
        this.channel = channel;
        this.aioCompletionHandler = aioCompletionHandler;
        this.writeCacheQueue = new ArrayBlockingQueue<ByteBuffer>(config.getWriteQueueSize());
        this.ioServerConfig = config;
        this.serverFlowLimit = serverSession ? false : null;
        config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);//触发状态机
        readAttach.buffer = ByteBuffer.allocate(config.getReadBufferSize());
        readFromChannel();//注册消息读事件
    }

    /**
     * 触发AIO的写操作,
     * <p>需要调用控制同步</p>
     */
    void writeToChannel() {
        ByteBuffer writeBuffer = writeAttach.buffer != null && writeAttach.buffer.hasRemaining() ? writeAttach.buffer : null;
        ByteBuffer nextBuffer = writeCacheQueue.peek();//为null说明队列已空
        if (writeBuffer == null && nextBuffer == null) {
            semaphore.release();
            if (isInvalid()) {//此时可能是Closing或Closed状态
                close();
            } else if (writeCacheQueue.size() > 0 && semaphore.tryAcquire()) {
                writeToChannel();
            }
            return;
        }

        //对缓存中的数据进行压缩处理再输出
        if (writeBuffer == null) {
            Iterator<ByteBuffer> iterable = writeCacheQueue.iterator();
            int totalSize = 0;
            while (iterable.hasNext() && totalSize <= 32 * 1024) {
                totalSize += iterable.next().remaining();
            }
            writeBuffer = ByteBuffer.allocate(totalSize);
            while (writeBuffer.hasRemaining()) {
                writeBuffer.put(writeCacheQueue.poll());
            }
            writeBuffer.flip();
        } else if (nextBuffer != null && nextBuffer.remaining() <= (writeBuffer.capacity() - writeBuffer.remaining())) {
            writeBuffer.compact();
            do {
                writeBuffer.put(writeCacheQueue.poll());
            }
            while ((nextBuffer = writeCacheQueue.peek()) != null && nextBuffer.remaining() <= writeBuffer.remaining());
            writeBuffer.flip();
        }

        writeAttach.buffer = writeBuffer;
        channel.write(writeBuffer, writeAttach, aioCompletionHandler);
    }

    public void write(final ByteBuffer buffer) throws IOException {
        if (isInvalid()) {
            throw new IOException("session is " + status);
        }
        if (future != null && !writeAttach.buffer.hasRemaining() && semaphore.availablePermits() == 0) {
            synchronized (this) {
                if (future != null && !writeAttach.buffer.hasRemaining() && semaphore.availablePermits() == 0) {
                    future = null;
                    semaphore.release();
                }
            }
        }
        if (writeCacheQueue.isEmpty() && semaphore.tryAcquire()) {
            writeAttach.buffer = buffer;
            future = channel.write(buffer);
            return;
        }
        try {
            //正常读取
            writeCacheQueue.put(buffer);
        } catch (InterruptedException e) {
            logger.error(e);
        }
        if (semaphore.tryAcquire()) {
            writeToChannel();
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
        status = immediate ? SESSION_STATUS_CLOSED : SESSION_STATUS_CLOSING;
        if (immediate) {
            try {
                channel.close();
                logger.debug("close connection:" + channel);
            } catch (IOException e) {
                logger.catching(e);
            }
        } else if (writeCacheQueue.isEmpty() && semaphore.tryAcquire()) {
            close(true);
            semaphore.release();
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

    /**
     * 如果存在流控并符合释放条件，则触发读操作
     */
    void tryReleaseFlowLimit() {
        if (serverFlowLimit != null && serverFlowLimit && writeCacheQueue.size() < ioServerConfig.getReleaseLine()) {
            serverFlowLimit = false;
            channel.read(readAttach.buffer, readAttach, aioCompletionHandler);
        }
    }

    /**
     * 触发通道的读操作，当发现存在严重消息积压时,会触发流控
     */
    void readFromChannel() {
        final ByteBuffer readBuffer = readAttach.buffer;
        readBuffer.flip();

        T dataEntry;
        while ((dataEntry = ioServerConfig.getProtocol().decode(readBuffer, this)) != null) {
            receive0(dataEntry);
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
        if (serverFlowLimit != null && writeCacheQueue.size() > ioServerConfig.getFlowLimitLine()) {
            serverFlowLimit = true;
        } else {
            channel.read(readBuffer, readAttach, aioCompletionHandler);
        }
    }

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public final void write(T t) throws IOException {
        write(ioServerConfig.getProtocol().encode(t, this));
    }

    /**
     * 接收并处理消息
     *
     * @param dataEntry 解码识别出的消息实体
     */
    private void receive0(T dataEntry) {
        try {
            for (SmartFilter<T> h : ioServerConfig.getFilters()) {
                h.processFilter(this, dataEntry);
            }
            ioServerConfig.getProcessor().process(this, dataEntry);
        } catch (Exception e) {
            logger.catching(e);
            for (SmartFilter<T> h : ioServerConfig.getFilters()) {
                h.processFailHandler(this, dataEntry, e);
            }
        }
    }

    class Attachment {
        /**
         * true:read,false:write
         */
        private final boolean read;
        private ByteBuffer buffer;

        public Attachment(boolean optType) {
            this.read = optType;
        }

        public AioSession getAioSession() {
            return AioSession.this;
        }

        public IoServerConfig getServerConfig() {
            return AioSession.this.ioServerConfig;
        }

        public boolean isRead() {
            return read;
        }
    }
}
