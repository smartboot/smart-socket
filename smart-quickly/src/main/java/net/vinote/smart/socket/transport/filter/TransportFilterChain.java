package net.vinote.smart.socket.transport.filter;

import net.vinote.smart.socket.transport.TransportSession;

/**
 * 
 * 传输层数据拦截器
 * 
 * @author Seer
 * @version TransportFilterChain.java, v 0.1 2015年8月28日 下午5:53:51 Seer Exp.
 */
public interface TransportFilterChain {

	/**
	 * 过滤连接事件
	 * 
	 * @param session
	 */
	public void doAcceptFilter(TransportSession session);
}
