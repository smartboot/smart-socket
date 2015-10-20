package net.vinote.smart.socket.service.filter.impl;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.SmartFilterChain;
import net.vinote.smart.socket.service.process.ProtocolDataReceiver;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 * @version SmartFilterChainImpl.java, v 0.1 2015年8月26日 下午5:08:31 Seer Exp.
 */
public class SmartFilterChainImpl implements SmartFilterChain {
	private ProtocolDataReceiver receiver;
	private SmartFilter[] handlers = null;

	public SmartFilterChainImpl(ProtocolDataReceiver receiver,
			SmartFilter[] handlers) {
		this.receiver = receiver;
		this.handlers = handlers;
	}

	public void doReadFilter(TransportSession session, List<DataEntry> dataList) {
		if (CollectionUtils.isEmpty(dataList)) {
			return;
		}
		if (handlers != null && handlers.length > 0) {
			for (SmartFilter h : handlers) {
				h.filterDataEntrys(session, dataList);
			}
		}
		for (DataEntry d : dataList) {
			// 接收到的消息进行预处理
			if (handlers != null && handlers.length > 0) {
				for (SmartFilter h : handlers) {
					h.readFilter(session, d);
				}
			}
			boolean succ = receiver.receive(session, d);
			// 未能成功接受消息
			if (!succ && handlers != null && handlers.length > 0) {
				for (SmartFilter h : handlers) {
					h.receiveFailHandler(session, d);
				}
			}

		}
	}

	public void doWriteFilter(TransportSession session, ByteBuffer buffer) {
		if (handlers != null && handlers.length > 0) {
			for (SmartFilter h : handlers) {
				h.writeFilter(session, buffer);
			}
		}
	}
}
