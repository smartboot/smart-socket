package net.vinote.smart.socket.protocol.p2p.server;

import java.util.concurrent.ArrayBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.server.P2PServerMessageProcessor.ProcessUnit;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;

class P2PServerProcessThread extends ProtocolProcessThread<BaseMessage> {
	private Logger logger = LogManager.getLogger(P2PServerProcessThread.class);
	private ArrayBlockingQueue<ProcessUnit> messageQueue;

	public P2PServerProcessThread(String name, ProtocolDataProcessor<BaseMessage> processor,
		ArrayBlockingQueue<ProcessUnit> queue) {
		super(name, processor);
		messageQueue = queue;
	}

	public void put(String sessionId, BaseMessage msg) {
		throw new UnsupportedOperationException("OMCServerProcessThread is not support put operation");
	}

	@Override
	public void run() {

		while (running) {
			
			try {
				ProcessUnit unit = messageQueue.take();
				processor.process(unit.session, unit.msg);
			} catch (Exception e) {
				if (running) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}
}