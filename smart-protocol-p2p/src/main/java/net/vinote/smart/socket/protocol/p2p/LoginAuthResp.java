package net.vinote.smart.socket.protocol.p2p;

import net.vinote.smart.socket.exception.DecodeException;

public class LoginAuthResp extends BaseMessage {

	private int resultCode;

	protected void encodeBody() {
		writeByte(MessageTag.RESULT_CODE);
		writeInt(resultCode);
	}

	protected void decodeBody() {
		byte tag = 0;
		int msgLen = getHead().getLength();
		while (getPosition() < msgLen) {
			// 读取tag值
			switch (tag = readByte()) {
			case MessageTag.RESULT_CODE:
				resultCode = readInt();
				break;
			default:
				throw new DecodeException("Invalid Tag value:" + tag);
			}
		}
	}

	public int getResultCode() {
		return resultCode;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public int getMessageType() {
		return MessageType.LOGIN_AUTH_RSP;
	}

	/**
	 * 登录鉴权响应码
	 * 
	 * @author Administrator
	 */
	public interface ResultCode {
		/** 鉴权成功 */
		int SUCCESS = 1000;

		/** 鉴权失败 */
		int FAILURE = 3000;

		/** 未鉴权 */
		int UN_AUTH = 3001;
	}
}
