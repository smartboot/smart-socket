package net.vinote.smart.socket.extension.cluster.trigger;

/**
 * 集群触发策略,所有消息都走集群
 * 
 * @author Seer
 *
 */
public class AllClusterTriggerStrategy implements ClusterTriggerStrategy {

	
	public boolean cluster() {
		return true;
	}

}
