package net.vinote.smart.socket.protocol.p2p;

import java.net.ProtocolException;
import java.security.InvalidParameterException;

import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.protocol.DataEntry;

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
	private DataEntry serviceMessage;

	protected void encodeBody() throws ProtocolException {
		writeString(clientUniqueNo);
		serviceMessage.encode();
		writeBytes(serviceMessage.getData());
	}

	protected void decodeBody() {
		clientUniqueNo = readString();
		byte[] data = readBytes();
		FragmentMessage tempMsg = new FragmentMessage();
		tempMsg.append(data, 0, data.length);
		serviceMessage = tempMsg.decodeMessage();
	}

	public int getMessageType() {
		return MessageType.CLUSTER_MESSAGE_REQ;
	}

	public void setServiceData(DataEntry data) {
		if (!(data instanceof BaseMessage)) {
			throw new InvalidParameterException("param must be instance of BaseMessage");
		}
		this.serviceMessage = (BaseMessage) data;
	}

	public DataEntry getServiceData() {
		return serviceMessage;
	}

	public void setUniqueNo(String no) {
		this.clientUniqueNo = no;
	}

	public String getUniqueNo() {
		return clientUniqueNo;
	}
}
