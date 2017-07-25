package net.vinote.smart.socket.transport;

import net.vinote.smart.socket.enums.ChannelStatusEnum;
import net.vinote.smart.socket.protocol.Protocol;
import net.vinote.smart.socket.service.filter.SmartFilterChain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定义底层通信管道会话<br/>
 *
 * @author Seer
 * @version Channel.java, v 0.1 2015年8月24日 上午10:31:38 Seer Exp.
 */
public abstract class IoSession<T> {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    /**
     * 唯一标识
     */
    private final int sessionId = NEXT_ID.getAndIncrement();

    /**
     * 本次读取的消息体大小
     */
    public static final String ATTRIBUTE_KEY_CUR_DATA_LENGTH = "_attr_key_curDataLength_";

    /**
     * 消息通信协议
     */
    protected Protocol<T> protocol;

    /**
     * 超时时间
     */
    protected int timeout;

    /**
     * 缓存大小
     */
    protected int bufferSize;

    /**
     * 缓存传输层读取到的数据流
     */
    protected ByteBuffer readBuffer;

    protected int cacheSize;
    private Map<String, Object> attribute = new HashMap<String, Object>();

    /**
     * 会话状态
     */
    private volatile ChannelStatusEnum status = ChannelStatusEnum.ENABLED;

    /**
     * 消息过滤器
     */
    protected SmartFilterChain<T> chain;

    /**
     * 读操作暂停标识
     */
    protected AtomicBoolean readPause = new AtomicBoolean(false);

    public IoSession(ByteBuffer readBuffer) {
        this.readBuffer = readBuffer;
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
            synchronized (IoSession.this) {
                close0();
                status = ChannelStatusEnum.CLOSED;
            }
        } else {
            status = ChannelStatusEnum.CLOSING;
        }
    }

    /**
     * * 关闭会话 *
     * <p>
     * * 会话的关闭将触发Socket通道的关闭 *
     * </p>
     */
    protected abstract void close0();

    /**
     * 刷新缓存的数据流,对已读取到的数据进行一次协议解码操作
     */
//    public ByteBuffer flushReadBuffer() {
//        //无可解析数据,直接返回
//        if (readBuffer.position() == 0 && readBuffer.limit() == readBuffer.capacity()) {
//            return readBuffer;
//        }
//        readBuffer.flip();
//
//        // 将从管道流中读取到的字节数据添加至当前会话中以便进行消息解析
//        T dataEntry;
//        while ((dataEntry = protocol.decode(readBuffer, this)) != null) {
//            chain.doReadFilter(this, dataEntry);
//        }
//        //数据读取完毕
//        if (readBuffer.remaining() == 0) {
//            readBuffer.clear();
//        } else if (readBuffer.position() > 0) {// 仅当发生数据读取时调用compact,减少内存拷贝
//            readBuffer.compact();
//        } else {
//            readBuffer.position(readBuffer.limit());
//            readBuffer.limit(readBuffer.capacity());
//        }
//        return readBuffer;
//    }


    /**
     * 获取当前Session的唯一标识
     *
     * @return
     */
    public final int getSessionID() {
        return sessionId;
    }

    public ChannelStatusEnum getStatus() {
        return status;
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
    public boolean isValid() {
        return status == ChannelStatusEnum.ENABLED;
    }

    /**
     * * 将参数中传入的数据输出至对端;处于性能考虑,通常对数据进行缓存处理
     *
     * @param data 输出数据至对端
     * @return 是否输出成功
     * @throws Exception
     */
    public abstract void write(ByteBuffer data) throws IOException;

    public final void write(T t) throws IOException {
        write(protocol.encode(t, this));
    }

    /**
     * * 将参数中传入的数据输出至对端;处于性能考虑,通常对数据进行缓存处理
     *
     * @param data
     *            输出数据至对
     * @return 是否输出成功
     * @throws Exception
     */
    // public abstract void write(T data) throws IOException;

    /**
     * Getter method for property <tt>attribute</tt>.
     *
     * @return property value of attribute
     */
    @SuppressWarnings("unchecked")
    public final <T1> T1 getAttribute(String key) {
        return (T1) attribute.get(key);
    }

    /**
     * Setter method for property <tt>attribute</tt>.
     *
     * @param key value to be assigned to property attribute
     */
    public final void setAttribute(String key, Object value) {
        attribute.put(key, value);
    }

    public final void removeAttribute(String key) {
        attribute.remove(key);
    }

    public AtomicBoolean getReadPause() {
        return readPause;
    }

    public SmartFilterChain<T> getFilterChain() {
        return chain;
    }
}
