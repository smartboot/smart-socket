package net.vinote.smart.socket.protocol.p2p;

/**
 * @author Seer
 * @version HeartMessageResp.java, v 0.1 2015年3月27日 下午2:23:38 Seer Exp.
 */
public class HeartMessageResp extends BaseMessage {

	public HeartMessageResp() {
		super();
	}

	public HeartMessageResp(HeadMessage head) {
		super(head);
	}

	public int getMessageType() {
		return MessageType.HEART_MESSAGE_RSP;
	}

	protected void encodeBody() {

	}

	protected void decodeBody() {

	}
}
