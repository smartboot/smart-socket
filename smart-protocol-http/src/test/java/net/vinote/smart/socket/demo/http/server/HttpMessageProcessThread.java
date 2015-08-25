package net.vinote.smart.socket.demo.http.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.http.RequestUnit;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;

class HttpMessageProcessThread extends ProtocolProcessThread {
	private ArrayBlockingQueue<RequestUnit> messageQueue;
	private Handler handler;

	public HttpMessageProcessThread(String name,
			ProtocolDataProcessor processor,
			ArrayBlockingQueue<RequestUnit> queue) {
		super(name, processor);
		this.messageQueue = queue;
	}

	public final void setHandler(Handler handler) {
		this.handler = handler;
	}

	public void put(String sessionId, DataEntry msg) {
		throw new UnsupportedOperationException(
				"OMCServerProcessThread is not support put operation");
	}

	public void run() {
		if (handler != null) {
			handler.handler();
		}
		while (running) {
			try {
				RequestUnit unit = messageQueue.take();
				processor.process(unit);
			} catch (Exception e) {
				if (running) {
					RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}
}

interface Handler {
	void handler();
}