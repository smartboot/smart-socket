package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.transport.IoSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 *
 */
public interface SmartFilterChain<T> {

	public void doReadFilter(IoSession<T> session, T buffer);

	/**
	 * 开始执行数据输出
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterStart(IoSession<T> session, ByteBuffer buffer);

	/**
	 * 再次输出剩余的数据
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterContinue(IoSession<T> session, ByteBuffer buffer);

	/**
	 * 完成数据输出
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterFinish(IoSession<T> session, ByteBuffer buffer);
}
