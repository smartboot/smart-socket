package net.vinote.smart.socket.extension.cluster.balance;

import net.vinote.smart.socket.transport.ClientChannelService;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 负载均衡接口
 * 
 * @author Seer
 *
 */
public interface LoadBalancing {

	/**
	 * 初始化负载均衡器
	 */
	public void registServer(ClientChannelService clusterSession);

	public TransportSession balancing(TransportSession clientSession);

	public void shutdown();
}
