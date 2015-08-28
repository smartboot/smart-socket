package net.vinote.smart.socket.protocol.p2p.message;

import net.vinote.smart.socket.exception.DecodeException;

public class LoginAuthReq extends BaseMessage {

	private byte encrypt;

	private String username;

	private String password;

	public LoginAuthReq() {
		super();
	}

	public LoginAuthReq(byte[] secureKey) {
		super(secureKey);
	}

	protected void encodeBody() {
		writeByte(MessageTag.ENCRYPT);
		writeByte(encrypt);

		writeByte(MessageTag.USERNAME);
		writeString(username);

		writeByte(MessageTag.PASSWORD);
		writeString(password);
	}

	protected void decodeBody() {
		byte tag = 0;
		int msgLen = getHead().getLength();
		while (getPosition() < msgLen) {
			// 读取tag值
			switch (tag = readByte()) {
			case MessageTag.ENCRYPT:
				encrypt = readByte();
				break;
			case MessageTag.USERNAME:
				username = readString();
				break;
			case MessageTag.PASSWORD:
				password = readString();
				break;
			default:
				throw new DecodeException("Invalid Tag value:" + tag);
			}
		}
	}

	public byte getEncrypt() {
		return encrypt;
	}

	public void setEncrypt(byte encrypt) {
		this.encrypt = encrypt;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getMessageType() {
		return MessageType.LOGIN_AUTH_REQ;
	}

}
