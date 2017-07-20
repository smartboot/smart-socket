package net.vinote.smart.socket.io.nio;

import net.vinote.smart.socket.enums.ChannelStatusEnum;
import net.vinote.smart.socket.io.Channel;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.SmartFilterChainImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 维护客户端-》服务端 或 服务端-》客户端 的当前会话
 *
 * @author Administrator
 */
public class NioChannel<T> extends Channel<T> {
    private static Logger logger = LogManager.getLogger(NioChannel.class);
    private SelectionKey channelKey = null;

    /**
     * 响应消息缓存队列
     */
    private ArrayBlockingQueue<ByteBuffer> writeCacheQueue;

    /**
     * 是否已注销读关注
     */
    private boolean readClosed = false;

    SessionWriteThread sessionWriteThread;
    SessionReadThread sessionReadThread;

    /**
     * @param channelKey 当前的Socket管道
     * @param config     配置
     */
    public NioChannel(SelectionKey channelKey, final QuicklyConfig<T> config) {
        super(ByteBuffer.allocateDirect(config.getDataBufferSize()));
        this.channelKey = channelKey;
        super.protocol = config.getProtocolFactory().createProtocol();
//        super.chain = new SmartFilterChainImpl<T>(config.getProcessor(), config.isServer() ? (SmartFilter<T>[]) ArrayUtils.add(config.getFilters(), new FlowControlFilter()) : config.getFilters());
        super.chain = new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters());
        super.cacheSize = config.getCacheSize();
        writeCacheQueue = new ArrayBlockingQueue<ByteBuffer>(cacheSize);
        super.bufferSize = config.getDataBufferSize();
        super.timeout = config.getTimeout();
    }

    @Override
    protected void cancelReadAttention() {
        readClosed = true;
        channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_READ);
    }

    @Override
    public void close(boolean immediate) {
        super.close(immediate || writeCacheQueue.isEmpty());
    }

    /*
     * (non-Javadoc)
     *
     * @see net.vinote.smart.socket.transport.TransportSession#close0()
     */
    @Override
    protected void close0() {
        writeCacheQueue.clear();
        if (getStatus() == ChannelStatusEnum.CLOSED) {
            return;
        }
        try {
            channelKey.channel().close();
            if (logger.isTraceEnabled()) {
                logger.trace("close connection " + channelKey.channel());
            }
        } catch (IOException e) {
            logger.warn("NioSession close channel Exception");
        }
        try {
            channelKey.cancel();
        } catch (Exception e) {
            logger.warn("NioSession cancel channelKey Exception");
        }
        try {
            channelKey.selector().wakeup();// 必须唤醒一次选择器以便移除该Key,否则端口会处于CLOSE_WAIT状态
        } catch (Exception e) {
            logger.warn("NioSession wakeup channelKey Exception");
        }
    }


    public int read(int readTimes) throws IOException {
        SocketChannel socketChannel = (SocketChannel) channelKey.channel();
        int readSize = 0;
        while (readTimes-- > 0) {
            readSize = socketChannel.read(readBuffer);
            if (readSize == 0 || readSize == -1) {
                break;
            }
            readBuffer.flip();

            // 将从管道流中读取到的字节数据添加至当前会话中以便进行消息解析
            T dataEntry;
            while ((dataEntry = protocol.decode(readBuffer, this)) != null) {
                chain.doReadFilter(this, dataEntry);
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

        return readSize;
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

    void flushWriteBuffer(int num) throws IOException {
        if (writeCacheQueue.isEmpty()) {
            return;
        }
        ByteBuffer[] array = new ByteBuffer[cacheSize];
        Iterator<ByteBuffer> iterable = writeCacheQueue.iterator();
        int i = 0;
        while (iterable.hasNext()) {
            array[i++] = iterable.next();
            if (i >= cacheSize) {
                break;
            }
        }
        ((SocketChannel) channelKey.channel()).write(array, 0, i);

        getWriteBuffer();
//        ByteBuffer buffer;
//        if (num <= 0) {
//            while ((buffer = getWriteBuffer()) != null) {
//                ((SocketChannel) channelKey.channel()).write(buffer);
//            }
//        } else {
//            while ((buffer = getWriteBuffer()) != null && ((SocketChannel) channelKey.channel()).write(buffer) > 0 && num-- > 0)
//                ;
//        }
    }


    @Override
    public final void pauseReadAttention() {
        if (!readPause.get() && (channelKey.interestOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
            channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_READ);
//            logger.info(getRemoteAddr() + ":" + getRemotePort() + "流控");
            readPause.set(true);
        }
    }

    @Override
    public final void resumeReadAttention() {
        if (readClosed || !readPause.get()) {
            return;
        }
        if ((channelKey.interestOps() & SelectionKey.OP_READ) != SelectionKey.OP_READ) {
            channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_READ);
            if (logger.isDebugEnabled()) {
//                logger.debug(getRemoteAddr() + ":" + getRemotePort() + "释放流控");
            }
        }
    }

    @Override
    public String toString() {
        return "Session [channel=" + channelKey.channel() + ", protocol=" + protocol + ", receiver=" + ", getClass()="
                + getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
    }

    @Override
    public final void write(ByteBuffer buffer) throws IOException {
        if (!isValid()) {
            return;
        }
        chain.doWriteFilterStart(this, buffer);
        buffer.flip();
// 队列为空时直接输出
//        if (writeCacheQueue.isEmpty()) {
//            synchronized (this) {
//                if (writeCacheQueue.isEmpty()) {
//                    // chain.doWriteFilter(this, buffer);
//                    int writeTimes = 8;// 控制循环次数防止低效输出流占用资源
//                    while (((SocketChannel) channelKey.channel()).write(buffer) > 0 && writeTimes >> 1 > 0)
//                        ;
//                    // 数据全部输出则return
//                    if (buffer.position() >= buffer.limit()) {
//                        chain.doWriteFilterFinish(this, buffer);
//                        return;
//                    }
//
//                    boolean cacheFlag = writeCacheQueue.offer(buffer);
//                    // 已输出部分数据，但剩余数据缓存失败,则异常处理
//                    if (!cacheFlag && buffer.position() > 0) {
//                        throw new IOException("cache data fail, channel has become unavailable!");
//                    }
//                    // 缓存失败并无数据输出,则忽略本次数据包
//                    if (!cacheFlag && buffer.position() == 0) {
//                        logger.warn("cache data fail, ignore!");
//                        return;
//                    }
//                    isNew = false;
//                }
//            }
//        }
        boolean suc = writeCacheQueue.offer(buffer);
        sessionWriteThread.notifySession(this);
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
     * 流控
     */
    class FlowControlFilter implements SmartFilter<T> {
        /**
         * 输入消息挤压量
         */
        private static final String READ_BACKLOG = "_READ_BACKLOG_";

        /**
         * 输出消息积压量
         */
        private static final String WRITE_BACKLOG = "_WRITE_BACKLOG_";

        /**
         * 消息发送次数
         */
        private static final String MESSAGE_SEND_TIMES = "_MESSAGE_SEND_TIMES_";

        @Override
        public void readFilter(Channel<T> session, T d) {
            //接收的消息积压量达到10个，则暂停读关注
            if (getReadBacklogCounter(session).incrementAndGet() > 10) {
//                System.out.println("readFilter 流控");
                session.pauseReadAttention();
            }
        }

        @Override
        public void processFilter(Channel<T> session, T d) {
            if (getReadBacklogCounter(session).decrementAndGet() == 0) {
//                System.out.println("processFilter 释放流控");
                session.resumeReadAttention();
            }
        }


        @Override
        public void beginWriteFilter(Channel<T> session, ByteBuffer d) {
            AtomicInteger counter = getWriteBacklogCounter(session);
            int num = counter.incrementAndGet();
            //已经存在消息挤压,暂停读关注
            if (num * 1.0 / session.getCacheSize() > 0.618) {
//                System.out.println("beginWriteFilter 流控");
                session.pauseReadAttention();
            }
        }

        @Override
        public void continueWriteFilter(Channel<T> session, ByteBuffer d) {
            int times = getMessageSendTimesCounter(session).incrementAndGet();
            //单条消息发送次数超过3次还未发完，说明网络有问题，暂停其读关注
            if (times > 3) {
//                System.out.println("continueWriteFilter 流控");
                session.pauseReadAttention();
            }
        }

        @Override
        public void finishWriteFilter(Channel<T> session, ByteBuffer d) {
            AtomicInteger counter = getWriteBacklogCounter(session);
            int num = counter.decrementAndGet();//释放积压量
            if (num == 0) {
//                System.out.println("finishWriteFilter 释放流控");
                session.resumeReadAttention();
            }
            getMessageSendTimesCounter(session).set(0);//清除记录
        }


        private AtomicInteger getReadBacklogCounter(Channel<T> session) {
            AtomicInteger counter = session.getAttribute(READ_BACKLOG);
            if (counter == null) {
                counter = new AtomicInteger();
                session.setAttribute(READ_BACKLOG, counter);
            }
            return counter;
        }

        private AtomicInteger getWriteBacklogCounter(Channel<T> session) {
            AtomicInteger counter = session.getAttribute(WRITE_BACKLOG);
            if (counter == null) {
                counter = new AtomicInteger();
                session.setAttribute(WRITE_BACKLOG, counter);
            }
            return counter;
        }

        private AtomicInteger getMessageSendTimesCounter(Channel<T> session) {
            AtomicInteger counter = session.getAttribute(MESSAGE_SEND_TIMES);
            if (counter == null) {
                counter = new AtomicInteger();
                session.setAttribute(MESSAGE_SEND_TIMES, counter);
            }
            return counter;
        }

        @Override
        public void receiveFailHandler(Channel<T> session, T d) {

        }
    }
}
