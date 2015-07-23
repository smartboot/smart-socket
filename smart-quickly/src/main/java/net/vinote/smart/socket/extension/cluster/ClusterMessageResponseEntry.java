package net.vinote.smart.socket.extension.cluster;


/**
 * 集群业务响应消息
 * 
 * @author Seer
 *
 */
public interface ClusterMessageResponseEntry extends ClusterMessageEntry {

	public boolean isSuccess();

	public String getInfo();
}
