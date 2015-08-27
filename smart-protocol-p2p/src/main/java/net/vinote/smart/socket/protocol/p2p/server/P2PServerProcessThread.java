package net.vinote.smart.socket.protocol.p2p.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.server.P2PServerMessageProcessor.ProcessUnit;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;
import net.vinote.smart.socket.service.session.SessionManager;

class P2PServerProcessThread extends ProtocolProcessThread {
	private ArrayBlockingQueue<ProcessUnit> messageQueue;

	public P2PServerProcessThread(String name, ProtocolDataProcessor processor,
			ArrayBlockingQueue<ProcessUnit> queue) {
		super(name, processor);
		messageQueue = queue;
	}

	public void put(String sessionId, BaseMessage msg) {
		throw new UnsupportedOperationException(
				"OMCServerProcessThread is not support put operation");
	}

	@Override
	public void run() {

		while (running) {
			SmartFilter[] handlers = processor.getQuicklyConfig().getFilters();
			try {
				ProcessUnit unit = messageQueue.take();
				if (handlers != null && handlers.length > 0) {
					for (SmartFilter h : handlers) {
						h.processFilter(SessionManager.getInstance()
								.getSession(unit.sessionId), unit.msg);
					}
				}
				processor.process(unit);
			} catch (Exception e) {
				if (running) {
					RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}
}