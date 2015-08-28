package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;

import net.vinote.smart.socket.exception.DecodeException;

/**
 * 请求公钥
 * 
 * @author Seer
 * @version PublicKeyReq.java, v 0.1 2015年8月26日 下午5:59:24 Seer Exp.
 */
public class SecureSocketMessageResp extends BaseMessage {
	private byte[] rsaPublicKey;

	/** 被加密的秘钥 */
	private byte[] encryptedKey;

	public SecureSocketMessageResp() {
		super();
	}

	public SecureSocketMessageResp(HeadMessage head) {
		super(head);
	}

	@Override
	protected void encodeBody() throws ProtocolException {
		writeBytes(rsaPublicKey);
		writeBytes(encryptedKey);
	}

	@Override
	protected void decodeBody() throws DecodeException {
		rsaPublicKey = readBytes();
		encryptedKey = readBytes();
	}

	public byte[] getRsaPublicKey() {
		return rsaPublicKey;
	}

	public void setRsaPublicKey(byte[] rsaPublicKey) {
		this.rsaPublicKey = rsaPublicKey;
	}

	public byte[] getEncryptedKey() {
		return encryptedKey;
	}

	public void setEncryptedKey(byte[] encryptedKey) {
		this.encryptedKey = encryptedKey;
	}

	/* (non-Javadoc)
	 * @see net.vinote.smart.socket.protocol.p2p.message.BaseMessage#getMessageType()
	 */
	@Override
	public int getMessageType() {
		return MessageType.SECURE_SOCKET_MESSAGE_RSP;
	}
}
