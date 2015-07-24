package net.vinote.smart.socket.protocol.p2p;

/**
 * 探测消息响应
 * 
 * @author Seer
 *
 */
public class DetectMessageResp extends BaseMessage {

	public DetectMessageResp() {
		super();
		// TODO Auto-generated constructor stub
	}

	public DetectMessageResp(HeadMessage head) {
		super(head);
		// TODO Auto-generated constructor stub
	}

	private String detectMessage;

	protected void encodeBody() {
		writeString(detectMessage);
	}

	protected void decodeBody() {
		detectMessage = readString();
	}

	public int getMessageType() {
		return MessageType.DETECT_MESSAGE_RSP;
	}

	public String getDetectMessage() {
		return detectMessage;
	}

	public void setDetectMessage(String detectMessage) {
		this.detectMessage = detectMessage;
	}
}
