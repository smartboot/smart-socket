package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.io.Channel;

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
	public void processFilter(Channel<T> session, T d);

	/**
	 * 消息接受前置预处理
	 *
	 * @param session
	 * @param d
	 */
	public void readFilter(Channel<T> session, T d);

	/**
	 * 消息接受失败处理
	 */
	public void receiveFailHandler(Channel<T> session, T d);

	/**
	 *消息输出前置处理
	 */
	public void beginWriteFilter(Channel<T> session, ByteBuffer d);

	/**
	 *消息输出中
	 */
	public void continueWriteFilter(Channel<T> session, ByteBuffer d);
	/**
	 *消息输出后置处理
	 */
	public void finishWriteFilter(Channel<T> session, ByteBuffer d);
}
