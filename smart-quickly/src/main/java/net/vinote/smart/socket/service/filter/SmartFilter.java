package net.vinote.smart.socket.service.filter;

import net.vinote.smart.socket.transport.IoSession;

import java.nio.ByteBuffer;

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
	public void processFilter(IoSession<T> session, T d);

	/**
	 * 消息接受前置预处理
	 *
	 * @param session
	 * @param d
	 */
	public void readFilter(IoSession<T> session, T d);

	/**
	 * 消息接受失败处理
	 */
	public void receiveFailHandler(IoSession<T> session, T d);

	/**
	 *消息输出前置处理
	 */
	public void beginWriteFilter(IoSession<T> session, ByteBuffer d);

	/**
	 *消息输出中
	 */
	public void continueWriteFilter(IoSession<T> session, ByteBuffer d);
	/**
	 *消息输出后置处理
	 */
	public void finishWriteFilter(IoSession<T> session, ByteBuffer d);
}
