package net.vinote.smart.socket.protocol.p2p.message;

/**
 * @author Seer
 * @version HeartMessageReq.java, v 0.1 2015年3月27日 下午2:23:43 Seer Exp.
 */
public class HeartMessageReq extends BaseMessage {
	private int transactionID;

	private String say;

	public void encodeBody() {
		writeInt(transactionID);
		writeString(say);
	}

	public void decodeBody() {
		transactionID = readInt();
		say = readString();
	}

	public String getSay() {
		return say;
	}

	public void setSay(String say) {
		this.say = say;
	}

	public int getTransactionID() {
		return transactionID;
	}

	public void setTransactionID(int transactionID) {
		this.transactionID = transactionID;
	}

	public int getMessageType() {
		return MessageType.HEART_MESSAGE_REQ;
	}
}
