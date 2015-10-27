package net.vinote.smart.socket.protocol.p2p.client;

import java.util.concurrent.ArrayBlockingQueue;

import net.vinote.smart.socket.exception.QueueOverflowStrategyException;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 业务消息处理线程
 *
 * @author Seer
 *
 */
class P2PClientProcessThread extends ProtocolProcessThread {
	private Logger logger = LoggerFactory.getLogger(P2PClientProcessThread.class);
	private QueueOverflowStrategy strategy;
	private ArrayBlockingQueue<BaseMessage> list = new ArrayBlockingQueue<BaseMessage>(1024);

	public P2PClientProcessThread(String name, ProtocolDataProcessor processor) {
		super(name, processor);
		strategy = QueueOverflowStrategy.valueOf(processor.getQuicklyConfig().getQueueOverflowStrategy());
	}

	public void put(String transId, BaseMessage msg) {
		switch (strategy) {
		case DISCARD:
			if (!list.offer(msg)) {
				logger.info("message queue is full!");
			}
			break;
		case WAIT:
			try {
				list.put(msg);
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
			break;
		default:
			throw new QueueOverflowStrategyException("Invalid overflow strategy " + strategy);
		}
	}

	@Override
	public void run() {

		while (running) {
			try {
				BaseMessage msg = list.take();
				processor.process(msg);
			} catch (Exception e) {
				if (running) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}

}
