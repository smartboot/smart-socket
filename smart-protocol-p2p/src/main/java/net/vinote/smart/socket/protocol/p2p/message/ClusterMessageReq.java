package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;

/**
 * 集群业务请求消息
 *
 * @author Seer
 * @version ClusterMessageReq.java, v 0.1 2015年3月27日 下午2:23:49 Seer Exp.
 */
public class ClusterMessageReq extends BaseMessage implements ClusterMessageEntry {

	/** 客户端唯一标识 */
	private String clientUniqueNo;
	/**
	 * 原始业务消息
	 */
	private ByteBuffer serviceMessage;

	@Override
	protected void encodeBody() throws ProtocolException {
		writeString(clientUniqueNo);
		writeBytes(serviceMessage.array());
	}

	@Override
	protected void decodeBody() {
		clientUniqueNo = readString();
		serviceMessage = ByteBuffer.wrap(readBytes());
	}

	@Override
	public int getMessageType() {
		return MessageType.CLUSTER_MESSAGE_REQ;
	}

	public void setServiceData(ByteBuffer data) {
		serviceMessage = data;
	}

	public ByteBuffer getServiceData() {
		return serviceMessage;
	}

	public void setUniqueNo(String no) {
		clientUniqueNo = no;
	}

	public String getUniqueNo() {
		return clientUniqueNo;
	}

}
