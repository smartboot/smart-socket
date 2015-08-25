package net.vinote.smart.socket.protocol.p2p.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

import net.vinote.smart.socket.exception.QueueOverflowStrategyException;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.p2p.BaseMessage;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;

/**
 * 
 * 业务消息处理线程
 * 
 * @author Seer
 *
 */
class P2PClientProcessThread extends ProtocolProcessThread {

	private QueueOverflowStrategy strategy;
	private ArrayBlockingQueue<BaseMessage> list = new ArrayBlockingQueue<BaseMessage>(
			1024);

	public P2PClientProcessThread(String name, ProtocolDataProcessor processor) {
		super(name, processor);
		strategy = QueueOverflowStrategy.valueOf(processor.getQuicklyConfig()
				.getQueueOverflowStrategy());
	}

	public void put(String transId, BaseMessage msg) {
		switch (strategy) {
		case DISCARD:
			if (!list.offer((BaseMessage) msg)) {
				RunLogger.getLogger().log(Level.FINE, "message queue is full!");
			}
			break;
		case WAIT:
			try {
				list.put((BaseMessage) msg);
			} catch (InterruptedException e) {
				RunLogger.getLogger().log(e);
			}
			break;
		default:
			throw new QueueOverflowStrategyException(
					"Invalid overflow strategy " + strategy);
		}
	}

	public void run() {

		while (running) {
			try {
				BaseMessage msg = list.take();
				processor.process(msg);
			} catch (Exception e) {
				if (running) {
					RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

}
