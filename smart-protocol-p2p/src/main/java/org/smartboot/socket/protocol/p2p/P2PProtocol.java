package org.smartboot.socket.protocol.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.HeadMessage;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.protocol.p2p.util.StringUtils;
import org.smartboot.socket.transport.AioSession;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

/**
 * Point to Point消息协议实现
 *
 * @author 三刀
 */
public final class P2PProtocol implements Protocol<BaseMessage> {
    /**
     * P2P消息标志性部分长度,消息头部的 幻数+消息大小 ,共8字节
     */
    private static final int MESSAGE_SIGN_LENGTH = 8;
    private static Logger LOGGER = LoggerFactory.getLogger(P2PProtocol.class);
    private P2pServiceMessageFactory serviceMessageFactory;

    public P2PProtocol(P2pServiceMessageFactory serviceMessageFactory) {
        this.serviceMessageFactory = serviceMessageFactory;
    }

    @Override
    public BaseMessage decode(ByteBuffer buffer, AioSession<BaseMessage> session) {
        // 未读取到数据则直接返回
        if (buffer == null || buffer.remaining() < MESSAGE_SIGN_LENGTH) {
            return null;
        }
        int magicNum = buffer.getInt(buffer.position() + 0);
        if (magicNum != HeadMessage.MAGIC_NUMBER) {
            byte[] bytes=new byte[buffer.remaining()];
            buffer.get(bytes);
            throw new DecodeException("Invalid Magic Number: 0x" + Integer.toHexString(magicNum) + "position:" + buffer.position() + " ,byteBuffer:"
                    + StringUtils.toHexString(bytes));
        }
        int msgLength = buffer.getInt(buffer.position() + 4);
        if (msgLength <= 0 || msgLength > buffer.capacity()) {
            throw new DecodeException("Invalid Message Length " + msgLength);
        }

        if (buffer.remaining() < msgLength) {
            return null;
        }
//        if (buffer.position() >= 964) {
//            System.err.println("position:" + buffer.position() + " ,remain:" + buffer.remaining() + " ,msglength:" + msgLength);
//        }
        BaseMessage message = decode(buffer);
        if (message == null) {
            throw new DecodeException("");
        }
        return message;
    }

    @Override
    public ByteBuffer encode(BaseMessage baseMessage, AioSession<BaseMessage> session) {
        try {
            return baseMessage.encode();
        } catch (ProtocolException e) {
            LOGGER.warn("", e);
        }
        return null;
    }

    private BaseMessage decode(ByteBuffer buffer) {
        int type = buffer.getInt(buffer.position() + 8);

        BaseMessage baseMsg = null;
        Class<?> c = serviceMessageFactory.getBaseMessage(type);
        if (c == null) {
            LOGGER.warn("Message[0x" + Integer.toHexString(type) + "] Could not find class");
            return null;
        }

        try {
            // 优先调用带HeadMessage参数的构造方法,减少BaseMessage中构造HeadMessage对象的次数
            baseMsg = (BaseMessage) c.newInstance();
        } catch (Exception e) {
            LOGGER.warn("", e);
        }
        if (baseMsg == null) {
            return null;
        }
        baseMsg.decode(buffer);
        return baseMsg;
    }
}
