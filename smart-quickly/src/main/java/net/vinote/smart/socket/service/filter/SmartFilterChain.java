package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 *
 */
public interface SmartFilterChain<T> {

	public void doReadFilter(TransportSession<T> session, T buffer);

	/**
	 * 开始执行数据输出
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterStart(TransportSession<T> session, ByteBuffer buffer);

	/**
	 * 再次输出剩余的数据
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterContinue(TransportSession<T> session, ByteBuffer buffer);

	/**
	 * 完成数据输出
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterFinish(TransportSession<T> session, ByteBuffer buffer);
}
