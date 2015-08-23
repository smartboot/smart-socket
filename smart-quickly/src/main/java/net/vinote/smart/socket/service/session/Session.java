package net.vinote.smart.socket.service.session;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 定义业务层会话接口
 *
 * @author Seer
 *
 */
public interface Session {

	/**
	 * Session的创建时间
	 *
	 * @return
	 */
	public long getCreationTime();

	/**
	 * 获取会话ID
	 *
	 * @return
	 */
	public String getId();

	/**
	 * 获取上一次接受客户端请求的时间
	 *
	 * @return
	 */
	public long getLastAccessedTime();

	/**
	 * 刷新访问时间
	 */
	public void refreshAccessedTime();

	/**
	 * 设置会话超时时间,若interval<=0,则该会话永不失效
	 *
	 * @param interval
	 */
	public void setMaxInactiveInterval(int interval);

	/**
	 * 获取会话的超时时长
	 *
	 * @return
	 */
	public int getMaxInactiveInterval();

	/**
	 * 获取会话属性
	 *
	 * @param name
	 * @return
	 */
	public Object getAttribute(String name);

	/**
	 * 设置会话属性
	 *
	 * @param name
	 * @param value
	 */
	public void setAttribute(String name, Object value);

	/**
	 * 移除会话属性
	 *
	 * @param name
	 */
	public void removeAttribute(String name);

	/**
	 * 失效当前会话
	 */
	public void invalidate();

	/**
	 * 失效当前会话
	 */
	public void invalidate(boolean immediate);

	/**
	 * 当前会话是否已失效
	 */
	public boolean isInvalid();

	/**
	 * 唤醒当前处于等待响应状态的请求
	 *
	 * @param baseMsg
	 *            响应消息
	 * @return
	 */
	public boolean notifySyncMessage(DataEntry baseMsg);

	/**
	 * 发生消息且不等待响应消息
	 *
	 * @param rspMsg
	 */
	public void sendWithoutResponse(DataEntry requestMsg) throws Exception;

	/**
	 * 发送消息并同步等待响应
	 *
	 * @param reqMsg
	 * @return
	 * @throws Exception
	 */
	public DataEntry sendWithResponse(DataEntry requestMsg) throws Exception;

	/**
	 * 发送消息并同步等待响应 /**
	 * 
	 * @param requestMsg
	 * @param timeout
	 *            超时时间
	 * @return
	 * @throws Exception
	 */
	public DataEntry sendWithResponse(DataEntry requestMsg, long timeout) throws Exception;

	/**
	 * 获取远程主机IP
	 *
	 * @return
	 */
	public String getRemoteIp();

	public String getLocalAddress();

	public TransportSession getTransportSession();
}
