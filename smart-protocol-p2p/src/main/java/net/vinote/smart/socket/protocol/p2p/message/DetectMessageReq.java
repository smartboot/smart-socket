package net.vinote.smart.socket.protocol.p2p.message;

import java.nio.ByteBuffer;

/**
 * 探测消息请求
 * 
 * @author Seer
 *
 */
public class DetectMessageReq extends BaseMessage {
	private String detectMessage;

	protected void encodeBody(ByteBuffer buffer) {
		writeString(buffer,detectMessage);
	}

	protected void decodeBody(ByteBuffer buffer) {
		detectMessage = readString(buffer);
	}

	public int getMessageType() {
		return MessageType.DETECT_MESSAGE_REQ;
	}

	public String getDetectMessage() {
		return detectMessage;
	}

	public void setDetectMessage(String detectMessage) {
		this.detectMessage = detectMessage;
	}
}
