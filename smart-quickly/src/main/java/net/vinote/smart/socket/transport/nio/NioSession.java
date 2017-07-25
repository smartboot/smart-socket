package net.vinote.smart.socket.transport.nio;

import net.vinote.smart.socket.enums.IoSessionStatusEnum;
import net.vinote.smart.socket.service.filter.impl.SmartFilterChainImpl;
import net.vinote.smart.socket.transport.IoSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 维护客户端-》服务端 或 服务端-》客户端 的当前会话
 *
 * @author Administrator
 */
public class NioSession<T> extends IoSession<T> {
    private static Logger logger = LogManager.getLogger(NioSession.class);
    private SelectionKey channelKey = null;

    /**
     * 响应消息缓存队列
     */
    private ArrayBlockingQueue<ByteBuffer> writeCacheQueue;

    SessionWriteThread sessionWriteThread;
    SessionReadThread sessionReadThread;

    /**
     * @param channelKey 当前的Socket管道
     * @param config     配置
     */
    public NioSession(SelectionKey channelKey, final IoServerConfig<T> config) {
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
        if (getStatus() == IoSessionStatusEnum.CLOSED) {
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


        if (buffer != null) {
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


}
