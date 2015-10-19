package net.vinote.smart.socket.extension.cluster;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.DataEntry;

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
	public void setServiceData(DataEntry data);

	/**
	 * 获取业务消息体
	 * 
	 * @return
	 */
	public DataEntry getServiceData();

	/**
	 * 设置集群业务消息的唯一标识
	 * 
	 * @param no
	 */
	public void setUniqueNo(String no);

	public String getUniqueNo();
	
	public void setQuicklyConfig(QuicklyConfig quicklyConfig);
}
