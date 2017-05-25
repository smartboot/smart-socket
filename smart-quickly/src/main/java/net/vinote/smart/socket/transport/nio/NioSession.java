package net.vinote.smart.socket.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.exception.NotYetReconnectedException;
import net.vinote.smart.socket.exception.QueueOverflowStrategyException;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.SmartFilterChainImpl;
import net.vinote.smart.socket.transport.TransportSession;
import net.vinote.smart.socket.transport.enums.SessionStatusEnum;

/**
 * 维护客户端-》服务端 或 服务端-》客户端 的当前会话
 *
 * @author Administrator
 */
public class NioSession<T> extends TransportSession<T> {
    private Logger logger = LogManager.getLogger(NioSession.class);
    private SelectionKey channelKey = null;

    /**
     * 响应消息缓存队列
     */
    private BlockingQueue<ByteBuffer> writeCacheQueue;

    private Object writeLock = new Object();

    private String remoteIp;
    private String remoteHost;
    private int remotePort;

    private String localAddress;

    /**
     * 是否已注销读关注
     */
    private boolean readClosed = false;

    private QueueOverflowStrategy strategy = QueueOverflowStrategy.DISCARD;
    /**
     * 是否自动修复链路
     */
    private boolean autoRecover;

    /**
     * @param channelKey 当前的Socket管道
     * @param config     配置
     */
    public NioSession(SelectionKey channelKey, final QuicklyConfig<T> config) {
        super(ByteBuffer.allocate(config.getDataBufferSize()));
        initBaseChannelInfo(channelKey);
        super.protocol = config.getProtocolFactory().createProtocol();
//        super.chain = new SmartFilterChainImpl<T>(config.getProcessor(), config.isServer() ? (SmartFilter<T>[]) ArrayUtils.add(config.getFilters(), new FlowControlFilter()) : config.getFilters());
        if(config.isServer()){
//            config.setFilters((SmartFilter<T>[]) ArrayUtils.add(config.getFilters(), new FlowControlFilter()));
        }
        super.chain = new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters());
        super.cacheSize = config.getCacheSize();
        writeCacheQueue = new ArrayBlockingQueue<ByteBuffer>(cacheSize);
        this.strategy = QueueOverflowStrategy.valueOf(config.getQueueOverflowStrategy());
        this.autoRecover = config.isAutoRecover();
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
        if (getStatus() == SessionStatusEnum.CLOSED) {
            return;
        }
        writeCacheQueue.clear();
        try {
            channelKey.channel().close();
            if (logger.isTraceEnabled()) {
                logger.trace("close connection " + channelKey.channel());
            }
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
        channelKey.cancel();
        channelKey.selector().wakeup();// 必须唤醒一次选择器以便移除该Key,否则端口会处于CLOSE_WAIT状态
    }

    @Override
    public String getLocalAddress() {
        return localAddress;
    }

    @Override
    public String getRemoteAddr() {
        return remoteIp;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public int getRemotePort() {
        return remotePort;
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

        // 缓存队列已空则注销写关注
        if (buffer == null) {
            synchronized (writeLock) {
                if (writeCacheQueue.isEmpty()) {
                    channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_WRITE);
                }
            }
            resumeReadAttention();
            return null;
        } /*
             * else if (buffer.position() == 0) {// 首次输出执行过滤器
			 * chain.doWriteFilter(this, buffer); }
			 */
        chain.doWriteFilterContinue(this, buffer);
        return buffer;
    }

    void flushWriteBuffer(int num) {
        ByteBuffer buffer;
        try {
            if(num<=0) {
                while ((buffer = getWriteBuffer()) != null) {
                    ((SocketChannel) channelKey.channel()).write(buffer);
                }
            }else{
                while ((buffer = getWriteBuffer()) != null&&((SocketChannel) channelKey.channel()).write(buffer)>0&&num-->0) ;
            }
        } catch (IOException e) {
            e.printStackTrace();
            close(true);
        }
    }

    void initBaseChannelInfo(SelectionKey channelKey) {
        Socket socket = ((SocketChannel) channelKey.channel()).socket();
        InetSocketAddress remoteAddr = (InetSocketAddress) socket.getRemoteSocketAddress();
        remoteIp = remoteAddr.getAddress().getHostAddress();
        localAddress = socket.getLocalAddress().getHostAddress();
        remotePort = remoteAddr.getPort();
        remoteHost = remoteAddr.getHostName();
        this.channelKey = channelKey;
    }

    @Override
    public boolean isValid() {
        return channelKey.isValid() && channelKey.channel().isOpen();
    }

    @Override
    public final void pauseReadAttention() {
        if ((channelKey.interestOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
            channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_READ);
            logger.info(getRemoteAddr() + ":" + getRemotePort() + "流控");
        }
    }

    @Override
    public final void resumeReadAttention() {
        if (readClosed) {
            return;
        }
        if ((channelKey.interestOps() & SelectionKey.OP_READ) != SelectionKey.OP_READ) {
            channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_READ);
            if (logger.isDebugEnabled()) {
                logger.debug(getRemoteAddr() + ":" + getRemotePort() + "释放流控");
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
        boolean isNew = true;

        buffer.flip();
        // 队列为空时直接输出
        if (writeCacheQueue.isEmpty()) {
            synchronized (this) {
                if (writeCacheQueue.isEmpty()) {
                    // chain.doWriteFilter(this, buffer);
                    int writeTimes = 8;// 控制循环次数防止低效输出流占用资源
                    while (((SocketChannel) channelKey.channel()).write(buffer) > 0 && writeTimes >> 1 > 0)
                        ;
                    // 数据全部输出则return
                    if (buffer.position() >= buffer.limit()) {
                        chain.doWriteFilterFinish(this, buffer);
                        return;
                    }

                    boolean cacheFlag = writeCacheQueue.offer(buffer);
                    // 已输出部分数据，但剩余数据缓存失败,则异常处理
                    if (!cacheFlag && buffer.position() > 0) {
                        throw new IOException("cache data fail, channel has become unavailable!");
                    }
                    // 缓存失败并无数据输出,则忽略本次数据包
                    if (!cacheFlag && buffer.position() == 0) {
                        logger.warn("cache data fail, ignore!");
                        return;
                    }
                    isNew = false;
                }
            }
        }
        // 若当前正阻塞于读操作，则尽最大可能进行写操作
        if (writeCacheQueue.remainingCapacity() <= 2) {
            flushWriteBuffer(0);
//            System.out.println("flush");
        }

        try {
            if (isNew) {
                switch (strategy) {
                    case DISCARD:
                        if (!writeCacheQueue.offer(buffer)) {
                            logger.warn("cache is full now," + StringUtils.toHexString(buffer.array()));
                        }
                        break;
                    case WAIT:
                        writeCacheQueue.put(buffer);
                        break;
                    default:
                        throw new QueueOverflowStrategyException("Invalid overflow strategy " + strategy);
                }
            }
        } catch (InterruptedException e) {
            logger.warn(e.getMessage(), e);
        } finally {
            if (channelKey.isValid()) {
                synchronized (writeLock) {
                    channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_WRITE);
                    channelKey.selector().wakeup();
                }
            } else {
                if (autoRecover) {
                    throw new NotYetReconnectedException("Network anomaly, will reconnect");
                } else {
                    writeCacheQueue.clear();
                    throw new IOException("Channel is invalid now!");
                }
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
        public void readFilter(TransportSession<T> session, T d) {
            //接收的消息积压量达到10个，则暂停读关注
            if (getReadBacklogCounter(session).incrementAndGet() > 10) {
//                System.out.println("readFilter 流控");
                session.pauseReadAttention();
            }
        }

        @Override
        public void processFilter(TransportSession<T> session, T d) {
            if (getReadBacklogCounter(session).decrementAndGet() == 0) {
//                System.out.println("processFilter 释放流控");
                session.resumeReadAttention();
            }
        }


        @Override
        public void beginWriteFilter(TransportSession<T> session, ByteBuffer d) {
            AtomicInteger counter = getWriteBacklogCounter(session);
            int num = counter.incrementAndGet();
            //已经存在消息挤压,暂停读关注
            if (num * 1.0 / session.getCacheSize() > 0.618) {
//                System.out.println("beginWriteFilter 流控");
                session.pauseReadAttention();
            }
        }

        @Override
        public void continueWriteFilter(TransportSession<T> session, ByteBuffer d) {
            int times = getMessageSendTimesCounter(session).incrementAndGet();
            //单条消息发送次数超过3次还未发完，说明网络有问题，暂停其读关注
            if (times > 3) {
//                System.out.println("continueWriteFilter 流控");
                session.pauseReadAttention();
            }
        }

        @Override
        public void finishWriteFilter(TransportSession<T> session, ByteBuffer d) {
            AtomicInteger counter = getWriteBacklogCounter(session);
            int num = counter.decrementAndGet();//释放积压量
            if (num == 0) {
//                System.out.println("finishWriteFilter 释放流控");
                session.resumeReadAttention();
            }
            getMessageSendTimesCounter(session).set(0);//清除记录
        }


        private AtomicInteger getReadBacklogCounter(TransportSession<T> session) {
            AtomicInteger counter = session.getAttribute(READ_BACKLOG);
            if (counter == null) {
                counter = new AtomicInteger();
                session.setAttribute(READ_BACKLOG, counter);
            }
            return counter;
        }

        private AtomicInteger getWriteBacklogCounter(TransportSession<T> session) {
            AtomicInteger counter = session.getAttribute(WRITE_BACKLOG);
            if (counter == null) {
                counter = new AtomicInteger();
                session.setAttribute(WRITE_BACKLOG, counter);
            }
            return counter;
        }

        private AtomicInteger getMessageSendTimesCounter(TransportSession<T> session) {
            AtomicInteger counter = session.getAttribute(MESSAGE_SEND_TIMES);
            if (counter == null) {
                counter = new AtomicInteger();
                session.setAttribute(MESSAGE_SEND_TIMES, counter);
            }
            return counter;
        }

        @Override
        public void receiveFailHandler(TransportSession<T> session, T d) {

        }
    }
}
