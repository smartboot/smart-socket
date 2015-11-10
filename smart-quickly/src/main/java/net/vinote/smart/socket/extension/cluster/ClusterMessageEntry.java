package net.vinote.smart.socket.extension.cluster;

import java.nio.ByteBuffer;

/**
 * 集群请求消息接口
 *
 * @author Seer
 *
 */
public interface ClusterMessageEntry {

	/**
	 * 设置业务消息体
	 *
	 * @param data
	 */
	public void setServiceData(ByteBuffer data);

	/**
	 * 获取业务消息体
	 *
	 * @return
	 */
	public ByteBuffer getServiceData();

	/**
	 * 设置集群业务消息的唯一标识
	 *
	 * @param no
	 */
	public void setUniqueNo(String no);

	public String getUniqueNo();

}
