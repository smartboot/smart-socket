package org.smartboot.socket.protocol.p2p.message;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.smartboot.socket.protocol.p2p.DecodeException;

import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * P2P协议基础消息体对象
 *
 * @author 三刀
 * @version BaseMessage.java, v 0.1 2015年8月22日 上午11:24:03 Seer Exp.
 */
public abstract class BaseMessage {
    /**
     * 消息头
     */
    private HeadMessage head;

    private static AtomicInteger sequence = new AtomicInteger(0);

    public BaseMessage(HeadMessage head) {
        this.head = head;
    }

    public BaseMessage() {
        this(new HeadMessage());
    }

    public final HeadMessage getHead() {
        return head;
    }

    /**
     * <p>
     * <p>
     * 消息编码;
     * <p>
     * </p>
     * <p>
     * <p>
     * <p>
     * 若是请求消息,将自动为其生成唯一标识 sequenceID;<br/>
     * <p>
     * 响应消息需要自行从对应的请求消息中获取再设置
     * <p>
     * </p>
     *
     * @throws ProtocolException
     */
    public final ByteBuffer encode() throws ProtocolException {
        if (head == null) {
            throw new ProtocolException("Protocol head is unset!");
        }
        // 完成消息体编码便可获取实际消息大小
        ByteBuffer bodyBuffer = ByteBuffer.allocate(512);
        bodyBuffer.position(HeadMessage.HEAD_MESSAGE_LENGTH);
        encodeBody(bodyBuffer);// 编码消息体
        bodyBuffer.flip();

        head.setLength(bodyBuffer.limit());// 设置消息体长度
        // 若是请求消息,将自动为其生成唯一标识 sequenceID;重复encode不产生新的序列号
        if (head.getSequenceID() == 0
                && (MessageType.RESPONSE_MESSAGE & getMessageType()) == MessageType.REQUEST_MESSAGE) {
            head.setSequenceID(sequence.incrementAndGet());// 由于初始值为0,所以必须先累加一次,否则获取到的是无效序列号
        }

        encodeHead(bodyBuffer);// 编码消息头
        bodyBuffer.position(bodyBuffer.limit());
        bodyBuffer.flip();
        return bodyBuffer;
    }

    /**
     * 消息解码
     *
     * @throws ProtocolException
     */
    public final void decode(ByteBuffer buffer) throws DecodeException {
        int bodyPosition = buffer.position() + HeadMessage.HEAD_MESSAGE_LENGTH;
        decodeHead(buffer);
        buffer.position(bodyPosition);
        decodeBody(buffer);
    }

    /**
     * 各消息类型各自实现消息体编码工作
     *
     * @throws ProtocolException
     */
    protected abstract void encodeBody(ByteBuffer buffer) throws ProtocolException;

    /**
     * 各消息类型各自实现消息体解码工作
     *
     * @param buffer
     * @throws ProtocolException
     */
    protected abstract void decodeBody(ByteBuffer buffer) throws DecodeException;

    /**
     * 获取消息类型
     *
     * @return
     */
    public abstract int getMessageType();

    /**
     * 对消息头进行编码
     */
    protected final void encodeHead(ByteBuffer buffer) {
        // 输出幻数
        buffer.putInt(HeadMessage.MAGIC_NUMBER);
        // 输出消息长度
        buffer.putInt(head.getLength());
        // 消息类型
        buffer.putInt(getMessageType());
        // 由发送方填写，请求和响应消息必须保持一致(4个字节)
        buffer.putInt(head.getSequenceID());

    }

    /**
     * 对消息头进行解码
     */
    protected final void decodeHead(ByteBuffer buffer) {
        // 读取幻数
        int magicNum = readInt(buffer);
        if (magicNum != HeadMessage.MAGIC_NUMBER) {
            throw new DecodeException("Invalid Magic Number: 0x" + Integer.toHexString(magicNum));
        }

        // 读取消息长度
        int length = readInt(buffer);

        // 消息类型
        int msgType = readInt(buffer);

        // 由发送方填写，请求和响应消息必须保持一致(4个字节)
        int sequeue = readInt(buffer);
        if (head == null) {
            head = new HeadMessage();
        }
        head.setLength(length);
        head.setMessageType(msgType);
        head.setSequenceID(sequeue);
    }

    /**
     * 输出字符串至数据体
     *
     * @param str
     */
    protected final void writeString(ByteBuffer buffer, String str) {
        writeBytes(buffer, str == null ? null : str.getBytes());
    }

    /**
     * 往数据块中输出byte数组
     *
     * @param data
     */
    protected final void writeBytes(ByteBuffer buffer, byte[] data) {
        if (data != null) {
            buffer.putInt(data.length);
            buffer.put(data);
        } else {
            buffer.putInt(-1);
        }
    }

    protected final void writeInt(ByteBuffer buffer, int value) {
        buffer.putInt(value);
    }

    protected final void writeLong(ByteBuffer buffer, long value) {
        buffer.putLong(value);
    }

    /**
     * 输出布尔值
     *
     * @param flag
     */
    protected final void writeBoolean(ByteBuffer buffer, boolean flag) {
        writeByte(buffer, flag ? (byte) 1 : 0);
    }

    /**
     * 往数据块中输入byte数值
     *
     * @param i
     */
    protected final void writeByte(ByteBuffer buffer, byte i) {
        buffer.put(i);
    }

    protected final byte readByte(ByteBuffer buffer) {
        return buffer.get();
    }

    /**
     * 从数据块的当前位置开始读取字符串
     *
     * @param buffer
     * @return
     */
    protected final String readString(ByteBuffer buffer) {
        return new String(readBytes(buffer));
    }

    /**
     * 从数据块中当前位置开始读取一个byte数值
     *
     * @return
     */
    protected final byte[] readBytes(ByteBuffer buffer) {
        int size = buffer.getInt();
        if (size < 0) {
            return null;
        }
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    protected final int readInt(ByteBuffer buffer) {
        return buffer.getInt();
    }

    protected final long readLong(ByteBuffer buffer) {
        return buffer.getLong();
    }

    /**
     * 读取一个布尔值
     *
     * @param buffer
     * @return
     */
    protected final boolean readBoolen(ByteBuffer buffer) {
        return buffer.get() == 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
    }

}