package net.vinote.smart.socket.protocol.p2p.message;

import net.vinote.smart.socket.exception.DecodeException;

/**
 * 鉴权响应消息
 * 
 * @author Seer
 * @version LoginAuthResp.java, v 0.1 2015年8月24日 下午6:18:34 Seer Exp.
 */
public class LoginAuthResp extends BaseMessage {

	public LoginAuthResp() {
		super();
	}

	public LoginAuthResp(HeadMessage head) {
		super(head);
	}

	/** 鉴权返回码 */
	private String returnCode;

	/** 鉴权返回信息 */
	private String returnDesc;

	protected void encodeBody() {
		writeByte(MessageTag.RETURN_CODE);
		writeString(returnCode);
		writeByte(MessageTag.DESC_TAG);
		writeString(returnDesc);
	}

	protected void decodeBody() {
		byte tag = 0;
		int msgLen = getHead().getLength();
		while (getPosition() < msgLen) {
			// 读取tag值
			switch (tag = readByte()) {
			case MessageTag.RETURN_CODE:
				returnCode = readString();
				break;
			case MessageTag.DESC_TAG:
				returnDesc = readString();
				break;
			default:
				throw new DecodeException("Invalid Tag value:" + tag);
			}
		}
	}

	public int getMessageType() {
		return MessageType.LOGIN_AUTH_RSP;
	}

	public String getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(String returnCode) {
		this.returnCode = returnCode;
	}

	public String getReturnDesc() {
		return returnDesc;
	}

	public void setReturnDesc(String returnDesc) {
		this.returnDesc = returnDesc;
	}

}
