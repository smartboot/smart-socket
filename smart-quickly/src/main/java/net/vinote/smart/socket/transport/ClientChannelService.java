package net.vinote.smart.socket.transport;

/**
 * 客户端传输服务
 * 
 * @author Seer
 * @version ClientChannelService.java, v 0.1 2015年3月20日 下午2:36:32 Seer Exp.
 */
public interface ClientChannelService extends ChannelService {
	/**
	 * 回去客户端传输会话
	 * 
	 * @return
	 */
	public TransportSession getSession();
}
