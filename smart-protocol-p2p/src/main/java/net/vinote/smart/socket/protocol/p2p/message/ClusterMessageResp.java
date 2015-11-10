package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

import net.vinote.smart.socket.extension.cluster.ClusterMessageResponseEntry;

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
	private ByteBuffer serviceRespMsg;

	@Override
	protected void encodeBody() throws ProtocolException {
		writeBoolean(success);
		writeString(info);
		writeString(clientUniqueNo);
		if (serviceRespMsg == null) {
			writeBytes(null);
		} else {
			writeBytes(serviceRespMsg.array());
		}
	}

	@Override
	protected void decodeBody() {
		success = readBoolen();
		info = readString();
		clientUniqueNo = readString();
		byte[] data = readBytes();
		if (data != null) {
			serviceRespMsg = ByteBuffer.wrap(data);
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

	@Override
	public int getMessageType() {
		return MessageType.CLUSTER_MESSAGE_RSP;
	}

	public String getUniqueNo() {
		return clientUniqueNo;
	}

	public void setServiceData(ByteBuffer data) {
		serviceRespMsg = data;
	}

	public ByteBuffer getServiceData() {
		// TODO Auto-generated method stub
		return serviceRespMsg;
	}

	public void setUniqueNo(String no) {
		clientUniqueNo = no;
	}

}
