package net.vinote.smart.socket.extension.cluster.balance;

import java.util.ArrayList;
import java.util.List;

import net.vinote.smart.socket.transport.ChannelService;
import net.vinote.smart.socket.transport.ClientChannelService;

/**
 * 负载均衡抽象实现类
 * 
 * @author Seer
 *
 */
public abstract class AbstractLoadBalancing implements LoadBalancing {

	protected List<ClientChannelService> serverList = new ArrayList<ClientChannelService>();

	public void registServer(ClientChannelService service) {
		serverList.add(service);
	}

	public void shutdown() {
		if (serverList != null) {
			for (ChannelService service : serverList) {
				service.shutdown();
			}
			serverList.clear();
		}
	}
}
