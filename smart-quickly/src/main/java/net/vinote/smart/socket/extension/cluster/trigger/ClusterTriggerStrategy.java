package net.vinote.smart.socket.extension.cluster.trigger;

/**
 * 集群触发策略
 * 
 * @author Seer
 *
 */
public interface ClusterTriggerStrategy {

	/**
	 * 是否集群连接
	 * 
	 * @return
	 */
	public boolean cluster();
}
