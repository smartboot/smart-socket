package net.vinote.smart.socket.extension.cluster.trigger;

/**
 * 集群触发策略,所有消息不走集群
 * 
 * @author Seer
 *
 */
public class NonClusterTriggerStrategy implements ClusterTriggerStrategy {

	
	public boolean cluster() {
		return false;
	}

}
