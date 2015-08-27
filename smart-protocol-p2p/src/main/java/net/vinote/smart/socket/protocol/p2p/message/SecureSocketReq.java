package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;

import net.vinote.smart.socket.exception.DecodeException;

/**
 * 安全通信请求消息,封装了非对称加密算法RSA的公钥
 * 
 * @author Seer
 * @version PublicKeyReq.java, v 0.1 2015年8月26日 下午5:59:24 Seer Exp.
 */
public class SecureSocketReq extends BaseMessage {
	/** RSA公钥 */
	private byte[] rsaPublicKey;

	@Override
	protected void encodeBody() throws ProtocolException {
		writeBytes(rsaPublicKey);
	}

	@Override
	protected void decodeBody() throws DecodeException {
		rsaPublicKey = readBytes();
	}

	public byte[] getRsaPublicKey() {
		return rsaPublicKey;
	}

	public void setRsaPublicKey(byte[] rsaPublicKey) {
		this.rsaPublicKey = rsaPublicKey;
	}

	@Override
	public int getMessageType() {
		return MessageType.REQUEST_MESSAGE | 0x99;
	}
}
