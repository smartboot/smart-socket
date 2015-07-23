package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.transport.TransportSession;

public interface ClientProcessor {

	/**
	 * 创建客户端会话
	 *
	 * @param session
	 */
	public void createSession(TransportSession session);
}
