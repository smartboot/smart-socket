package net.vinote.smart.socket.protocol.p2p.message;


/**
 * 服务端与客户端断连请求
 * 
 * @author Administrator
 * 
 */
public class DisconnectReq extends BaseMessage {

	protected void encodeBody() {

	}

	protected void decodeBody() {

	}

	public int getMessageType() {
		return MessageType.DIS_CONNECT_REQ;
	}

}
