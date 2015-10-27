package net.vinote.smart.socket.extension.cluster.balance;

import java.util.concurrent.atomic.AtomicInteger;

import net.vinote.smart.socket.transport.ClientChannelService;
import net.vinote.smart.socket.transport.TransportSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 轮循算法
 * <p>
 * 说明:每一次来自网络的请求轮流分配给内部中的每台服务器,从1至N然后重新开始｡
 * </p>
 * <p>
 * 举例:此种负载均衡算法适合于服务器组中的所有服务器都有相同的软硬件配置并且平均服务请求相对均衡的情况;
 * </p>
 *
 * @author Seer
 * @version RoundRobinLoad.java, v 0.1 2015年8月25日 下午3:22:18 Seer Exp.
 */
public class RoundRobinLoad extends AbstractLoadBalancing {
	private Logger logger = LoggerFactory.getLogger(RoundRobinLoad.class);
	private AtomicInteger index = new AtomicInteger(0);

	public TransportSession balancing(TransportSession clientSession) {
		ClientChannelService clusterClient = serverList.get(index.getAndIncrement() % serverList.size());
		TransportSession session = clusterClient.getSession();
		logger.info("distribute ClusterServer[IP:" + session.getRemoteAddr() + " ,Port:" + session.getRemotePort()
			+ "] to Client[IP:" + clientSession.getRemoteAddr() + " ,Port:" + clientSession.getRemotePort() + "]");
		return session;
	}
}
