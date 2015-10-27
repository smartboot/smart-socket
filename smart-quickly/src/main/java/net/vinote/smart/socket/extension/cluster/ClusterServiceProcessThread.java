package net.vinote.smart.socket.extension.cluster;

import java.util.concurrent.ArrayBlockingQueue;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClusterServiceProcessThread extends ProtocolProcessThread {
	private Logger logger = LoggerFactory.getLogger(ClusterServiceProcessThread.class);
	private ArrayBlockingQueue<ProcessUnit> messageQueue;

	public ClusterServiceProcessThread(String name, ProtocolDataProcessor processor,
		ArrayBlockingQueue<ProcessUnit> queue) {
		super(name, processor);
		messageQueue = queue;
	}

	public void put(String sessionId, DataEntry msg) {
		throw new UnsupportedOperationException("OMCServerProcessThread is not support put operation");
	}

	@Override
	public void run() {

		while (running) {
			SmartFilter[] handlers = processor.getQuicklyConfig().getFilters();
			try {
				ProcessUnit unit = messageQueue.take();
				if (handlers != null && handlers.length > 0) {
					for (SmartFilter h : handlers) {
						h.processFilter(null, unit.msg.getServiceData());
					}
				}
				processor.process(unit);
			} catch (Exception e) {
				if (running) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}
}