package net.vinote.smart.socket.transport.filter;

import net.vinote.smart.socket.transport.TransportSession;

/**
 * 传输层过滤器
 * 
 * @author Seer
 * @version TransportFitler.java, v 0.1 2015年8月28日 下午5:47:36 Seer Exp.
 */
public interface TransportFilter {

	/**
	 * 接受连接事件
	 * 
	 * @param session
	 */
	public void doAccecpt(TransportSession session);
}
