package net.vinote.smart.socket.protocol.p2p.message;

/**
 * 异常码流
 * 
 * @author Seer
 * @version InvalidMessageReq.java, v 0.1 2015年3月27日 下午2:23:31 Seer Exp.
 */
public class InvalidMessageReq extends BaseMessage {

	private String msg;
	private byte[] invalidMsgData;

	protected void encodeBody() {
		writeString(msg);
		writeBytes(invalidMsgData);
	}

	protected void decodeBody() {
		msg = readString();
		invalidMsgData = readBytes();
	}

	public int getMessageType() {
		return MessageType.INVALID_MESSAGE_REQ;
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
