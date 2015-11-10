package net.vinote.smart.socket.protocol;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

import net.vinote.smart.socket.exception.DecodeException;
import net.vinote.smart.socket.protocol.p2p.message.FragmentMessage;
import net.vinote.smart.socket.protocol.p2p.message.HeadMessage;
import net.vinote.smart.socket.protocol.p2p.message.InvalidMessageReq;
import net.vinote.smart.socket.transport.TransportSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Point to Point消息协议实现
 *
 * @author Administrator
 *
 */
final class P2PProtocol implements Protocol {
	private Logger logger = LoggerFactory.getLogger(P2PProtocol.class);
	/**
	 * P2P消息标志性部分长度,消息头部的 幻数+消息大小 ,共8字节
	 */
	private static final int MESSAGE_SIGN_LENGTH = 8;

	private static final String FRAGMENT_MESSAGE_KEY = "fmk";

	public ByteBuffer decode(ByteBuffer buffer, TransportSession session) {
		// 未读取到数据则直接返回
		if (buffer == null || buffer.remaining() < MESSAGE_SIGN_LENGTH) {
			return null;
		}
		int magicNum = buffer.getInt(0);
		if (magicNum != HeadMessage.MAGIC_NUMBER) {
			throw new DecodeException("Invalid Magic Number: 0x" + Integer.toHexString(magicNum));
		}
		int msgLength = buffer.getInt(4);
		if (msgLength <= 0) {
			throw new DecodeException("Invalid Message Length " + msgLength);
		}

		if (buffer.remaining() < msgLength) {
			return null;
		}
		byte[] data = new byte[msgLength];
		buffer.get(data);
		return ByteBuffer.wrap(data);
	}

	public DataEntry wrapInvalidProtocol(TransportSession session) {
		InvalidMessageReq msg = new InvalidMessageReq();
		msg.setMsg("Invalid P2P Message.");
		FragmentMessage fragMsg = (FragmentMessage) session.getAttribute(FRAGMENT_MESSAGE_KEY);
		if (fragMsg != null) {
			msg.setInvalidMsgData(fragMsg.getData().array());
		}
		try {
			msg.encode();
		} catch (ProtocolException e) {
			logger.warn("", e);
		}
		return msg;
	}
}
