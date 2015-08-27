package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;

import net.vinote.smart.socket.extension.cluster.ClusterMessageResponseEntry;
import net.vinote.smart.socket.protocol.DataEntry;

/**
 * 集群业务响应消息
 * 
 * @author Seer
 *
 */
public class ClusterMessageResp extends BaseMessage implements ClusterMessageResponseEntry {

	/**
	 * 是否成功
	 */
	private boolean success;

	/**
	 * 提示信息
	 */
	private String info;
	/** 客户端唯一标识 */
	private String clientUniqueNo;
	/**
	 * 业务响应消息
	 */
	private DataEntry serviceRespMsg;

	protected void encodeBody() throws ProtocolException {
		writeBoolean(success);
		writeString(info);
		writeString(clientUniqueNo);
		if (serviceRespMsg == null) {
			writeBytes(null);
		} else {
			serviceRespMsg.encode();
			writeBytes(serviceRespMsg.getData());
		}
	}

	protected void decodeBody() {
		success = readBoolen();
		info = readString();
		clientUniqueNo = readString();
		byte[] data = readBytes();
		if (data != null) {
			FragmentMessage tempMsg = new FragmentMessage();
			tempMsg.append(data, 0, data.length);
			serviceRespMsg = tempMsg.decodeMessage();
		}
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public int getMessageType() {
		return MessageType.CLUSTER_MESSAGE_RSP;
	}

	public String getUniqueNo() {
		return clientUniqueNo;
	}

	public void setServiceData(DataEntry data) {
		this.serviceRespMsg = data;
	}

	public DataEntry getServiceData() {
		// TODO Auto-generated method stub
		return serviceRespMsg;
	}

	public void setUniqueNo(String no) {
		this.clientUniqueNo = no;
	}
}
