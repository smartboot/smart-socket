package net.vinote.smart.socket.extension.cluster;

import net.vinote.smart.socket.transport.TransportSession;

class ProcessUnit {
	TransportSession clientSession;// 与客户端的链路会话
	TransportSession clusterSession;// 与集群服务器的链路会话
	ClusterMessageEntry msg;

	public ProcessUnit(TransportSession clientSession,
			TransportSession clusterSession, ClusterMessageEntry msg) {
		this.clientSession = clientSession;
		this.clusterSession = clusterSession;
		this.msg = msg;
	}
}