package net.vinote.smart.socket.transport.filter.impl;

import net.vinote.smart.socket.transport.TransportSession;
import net.vinote.smart.socket.transport.filter.TransportFilter;

/**
 * 检测链路数据是否保持活跃,若长时间无数据交互，将关闭网络
 * 
 * @author Seer
 * @version TransportAliveFilter.java, v 0.1 2015年8月28日 下午6:14:00 Seer Exp.
 */
public class TransportAliveFilter implements TransportFilter {

	@Override
	public void doAccecpt(TransportSession session) {

	}

}
