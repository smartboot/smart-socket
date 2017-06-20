package net.vinote.smart.socket.service;

/**
 * 业务层Session对象
 * @author zhengjunwei
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
}
