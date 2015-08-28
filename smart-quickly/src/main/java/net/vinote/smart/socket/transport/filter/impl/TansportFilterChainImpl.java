package net.vinote.smart.socket.transport.filter.impl;

import net.vinote.smart.socket.transport.TransportSession;
import net.vinote.smart.socket.transport.filter.TransportFilter;
import net.vinote.smart.socket.transport.filter.TransportFilterChain;

import org.apache.commons.lang3.ArrayUtils;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 * @version SmartFilterChainImpl.java, v 0.1 2015年8月26日 下午5:08:31 Seer Exp.
 */
public class TansportFilterChainImpl implements TransportFilterChain {
	private TransportFilter[] handlers = null;

	public TansportFilterChainImpl(TransportFilter[] handlers) {
		this.handlers = handlers;
	}

	@Override
	public void doAcceptFilter(TransportSession session) {
		if (ArrayUtils.isNotEmpty(handlers)) {
			for (TransportFilter h : handlers) {
				h.doAccecpt(session);
			}
		}

	}
}
