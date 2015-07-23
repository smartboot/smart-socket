package net.vinote.smart.socket.protocol.p2p;


/**
 * 探测消息请求
 * 
 * @author Seer
 *
 */
public class DetectMessageReq extends BaseMessage {
	private String detectMessage;

	protected void encodeBody() {
		writeString(detectMessage);
	}

	protected void decodeBody() {
		detectMessage = readString();
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
