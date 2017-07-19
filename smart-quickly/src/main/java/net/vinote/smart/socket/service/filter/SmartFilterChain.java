package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.io.Channel;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 *
 */
public interface SmartFilterChain<T> {

	public void doReadFilter(Channel<T> session, T buffer);

	/**
	 * 开始执行数据输出
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterStart(Channel<T> session, ByteBuffer buffer);

	/**
	 * 再次输出剩余的数据
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterContinue(Channel<T> session, ByteBuffer buffer);

	/**
	 * 完成数据输出
	 * @param session
	 * @param buffer
	 */
	public void doWriteFilterFinish(Channel<T> session, ByteBuffer buffer);
}
