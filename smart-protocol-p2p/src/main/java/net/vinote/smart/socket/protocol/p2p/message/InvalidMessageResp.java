package net.vinote.smart.socket.protocol.p2p.message;

/**
 * 异常码流响应
 * 
 * @author Seer
 * @version InvalidMessageResp.java, v 0.1 2015年3月16日 下午3:46:35 Seer Exp.
 */
public class InvalidMessageResp extends BaseMessage {

	private String msg;
	private byte[] invalidMsgData;

	public InvalidMessageResp() {
		super();
	}

	public InvalidMessageResp(HeadMessage head) {
		super(head);
	}

	protected void encodeBody() {
		writeString(msg);
		writeBytes(invalidMsgData);
	}

	protected void decodeBody() {
		msg = readString();
		invalidMsgData = readBytes();
	}

	public int getMessageType() {
		return MessageType.INVALID_MESSAGE_RSP;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public byte[] getInvalidMsgData() {
		return invalidMsgData;
	}

	public void setInvalidMsgData(byte[] invalidMsgData) {
		this.invalidMsgData = invalidMsgData;
	}
}
