package net.vinote.smart.socket.protocol.p2p;

/**
 * 服务端与客户端断连请求
 * 
 * @author Seer
 * @version DisconnectResp.java, v 0.1 2015年3月27日 下午2:24:04 Seer Exp.
 */
public class DisconnectResp extends BaseMessage {

	protected void encodeBody() {

	}

	protected void decodeBody() {

	}

	public int getMessageType() {
		return MessageType.DIS_CONNECT_RSP;
	}

}
