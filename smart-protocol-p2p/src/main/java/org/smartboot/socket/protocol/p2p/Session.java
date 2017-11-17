package org.smartboot.socket.protocol.p2p;

/**
 * 定义业务层Session对象。
 * 由于传输层提供的IoSession是脱离于业务的，其接口存在一定的复杂度，直接使用可能会出现不可预知的问题。
 * 建议于业务层的Session接口实现类对其做一层封装
 * @author 三刀
 *
 * @param <T>
 */
public interface Session<T> {
	
	/**
	 * 发送异步消息
	 * @param requestMsg
	 * @throws Exception
	 */
	public void sendWithoutResponse(T requestMsg) throws Exception;

	/**
	 * 发送同步消息，等待响应消息
	 * @param requestMsg
	 * @return
	 * @throws Exception
	 */
	public T sendWithResponse(T requestMsg) throws Exception;

	/**
	 * 发送同步消息，等待响应消息，并设置了等待超时时间
	 * @param requestMsg
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public T sendWithResponse(T requestMsg, long timeout) throws Exception;

	/**
	 * 唤醒同步消息
	 * @param baseMsg
	 * @return
	 */
	public boolean notifySyncMessage(T baseMsg);

	void close();

	void close(boolean immediate);
}
