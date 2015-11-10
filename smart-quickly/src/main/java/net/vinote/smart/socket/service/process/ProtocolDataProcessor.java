package net.vinote.smart.socket.service.process;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 协议消息处理器
 *
 * @author Seer
 * @version ProtocolDataProcessor.java, v 0.1 2015年3月13日 下午3:26:55 Seer Exp.
 */
public interface ProtocolDataProcessor extends ProtocolDataReceiver {
	/**
	 * 仅服务器端可实现该方法,客户端无此必要
	 *
	 * @param data
	 * @return
	 */
	public ClusterMessageEntry generateClusterMessage(ByteBuffer data);

	/**
	 * 获取服务器/客户端配置
	 *
	 * @return
	 */
	public QuicklyConfig getQuicklyConfig();

	/**
	 * 初始化处理器
	 *
	 * @throws Exception
	 */
	public void init(QuicklyConfig config) throws Exception;

	/**
	 * 用于处理指定session内的一个消息实例,若直接在该方法内处理消息,则实现的是同步处理方式.
	 * 若需要采用异步，则介意此方法的实现仅用于接收消息，至于消息处理则在其他线程中实现
	 *
	 * @param session
	 * @throws Exception
	 */
	public <T> void process(T session) throws Exception;

	/**
	 * 关闭处理器
	 */
	public void shutdown();

	/**
	 * 获取业务层会话
	 *
	 * @param tsession
	 * @return
	 */
	public Session getSession(TransportSession tsession);
}
