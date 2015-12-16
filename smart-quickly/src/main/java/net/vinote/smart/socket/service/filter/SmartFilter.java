package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 *
 */
public interface SmartFilter<T> {

	/**
	 * 消息处理前置预处理
	 *
	 * @param session
	 * @param d
	 */
	public void processFilter(TransportSession<T> session, T d);

	/**
	 * 消息接受前置预处理
	 *
	 * @param session
	 * @param d
	 */
	public void readFilter(TransportSession<T> session, T d);

	/**
	 * 消息接受失败处理
	 */
	public void receiveFailHandler(TransportSession<T> session, T d);

	public void writeFilter(TransportSession<T> session, ByteBuffer d);
}
