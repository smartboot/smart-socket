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

	public DataEntry decode(ByteBuffer buffer, TransportSession session) {
		// 未读取到数据则直接返回
		if (buffer == null) {
			return null;
		}
		// 获取消息片段对象
		FragmentMessage tempMsg = (FragmentMessage) session.getAttribute(FRAGMENT_MESSAGE_KEY);
		if (tempMsg == null) {
			tempMsg = new FragmentMessage();
			session.setAttribute(FRAGMENT_MESSAGE_KEY, tempMsg);
		}
		while (buffer.hasRemaining()) {
			int min;
			if (tempMsg.getReadSize() < MESSAGE_SIGN_LENGTH) {
				min = Math.min(buffer.remaining(), MESSAGE_SIGN_LENGTH - tempMsg.getReadSize());
				tempMsg.append(buffer, min);
			}
			// 先解析消息体大小
			if (tempMsg.getLength() == 0 && tempMsg.getReadSize() >= MESSAGE_SIGN_LENGTH) {
				byte[] intBytes = new byte[4];
				// 解析幻数
				for (int i = 0; i < intBytes.length; i++) {
					intBytes[i] = tempMsg.getData()[i];
				}
				int magicNum = getInt(intBytes);
				if (magicNum != HeadMessage.MAGIC_NUMBER) {
					throw new DecodeException("Invalid Magic Number: 0x" + Integer.toHexString(magicNum));
				}

				// 解析消息体大小,第四位开始
				for (int i = 0; i < intBytes.length; i++) {
					intBytes[i] = tempMsg.getData()[i + 4];
				}
				int msgLength = getInt(intBytes);
				if (msgLength <= 0) {
					throw new DecodeException("Invalid Message Length " + msgLength);
				}
				tempMsg.setLength(msgLength);
			}

			min = Math.min(tempMsg.getLength() - tempMsg.getReadSize(), buffer.remaining());
			if (min > 0) {
				tempMsg.append(buffer, min);
				if (tempMsg.getLength() == tempMsg.getReadSize()) {
					session.removeAttribute(FRAGMENT_MESSAGE_KEY);
					return tempMsg;
				}
			}
		}
		return null;
	}

	private int getInt(byte[] data) {
		if (data == null || data.length != 4) {
			throw new RuntimeException("data length is must 4!");
		}
		int index = 0;
		return ((data[index++] & 0xff) << 24) + ((data[index++] & 0xff) << 16) + ((data[index++] & 0xff) << 8)
			+ (data[index++] & 0xff);
	}

	public DataEntry wrapInvalidProtocol(TransportSession session) {
		InvalidMessageReq msg = new InvalidMessageReq();
		msg.setMsg("Invalid P2P Message.");
		FragmentMessage fragMsg = (FragmentMessage) session.getAttribute(FRAGMENT_MESSAGE_KEY);
		if (fragMsg != null) {
			msg.setInvalidMsgData(fragMsg.getData());
		}
		try {
			msg.encode();
		} catch (ProtocolException e) {
			logger.warn("", e);
		}
		return msg;
	}
}
