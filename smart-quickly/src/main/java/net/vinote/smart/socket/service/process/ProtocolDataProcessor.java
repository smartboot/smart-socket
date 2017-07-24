package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.service.Session;

/**
 * 协议消息处理器
 *
 * @author Seer
 * @version ProtocolDataProcessor.java, v 0.1 2015年3月13日 下午3:26:55 Seer Exp.
 */
public interface  ProtocolDataProcessor<T> extends ProtocolDataReceiver<T> {

	/**
	 * 初始化处理器
	 *
	 * @throws Exception
	 */
	public void init(int threadNum);

	/**
	 * 用于处理指定session内的一个消息实例,若直接在该方法内处理消息,则实现的是同步处理方式.
	 * 若需要采用异步，则介意此方法的实现仅用于接收消息，至于消息处理则在其他线程中实现
	 *
	 * @param session
	 * @throws Exception
	 */
	public void process(Session<T> session, T msg) throws Exception;

	/**
	 * 关闭处理器
	 */
	public void shutdown();

}
