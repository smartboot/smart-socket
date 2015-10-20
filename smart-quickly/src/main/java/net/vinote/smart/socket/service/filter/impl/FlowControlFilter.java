package net.vinote.smart.socket.service.filter.impl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.CollectionUtils;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 流量控制过滤器
 *
 * @author Seer
 * @version FlowControlFilter.java, v 0.1 2015年4月11日 下午4:29:24 Seer Exp.
 */
public class FlowControlFilter implements SmartFilter {
	private WeakHashMap<TransportSession, AtomicInteger> sessionMap = new WeakHashMap<TransportSession, AtomicInteger>();

	@Override
	public void filterDataEntrys(TransportSession session, List<DataEntry> d) {
		if (CollectionUtils.isNotEmpty(d)) {
			AtomicInteger counter = getCounter(session);
			if (session.getQuickConfig().isServer()) {
				int count = counter.addAndGet(d.size());
				if (count * 1.0 / session.getQuickConfig().getCacheSize() > 0.618) {
					session.pauseReadAttention();
				}
			}
		}
	}

	private AtomicInteger getCounter(TransportSession session) {
		AtomicInteger counter = sessionMap.get(session);
		if (counter == null) {
			counter = new AtomicInteger();
			sessionMap.put(session, counter);
		}
		return counter;
	}

	@Override
	public void processFilter(Session session, DataEntry d) {
	}

	@Override
	public void readFilter(TransportSession session, DataEntry d) {
	}

	@Override
	public void receiveFailHandler(TransportSession session, DataEntry d) {
	}

	@Override
	public void writeFilter(TransportSession session, ByteBuffer d) {
		AtomicInteger counter = getCounter(session);
		if (session.getQuickConfig().isServer()) {
			int num = counter.decrementAndGet();
			if (num * 1.0 / session.getQuickConfig().getCacheSize() < 0.382) {
				session.resumeReadAttention();
			}
		}
	}
}
