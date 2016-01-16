package net.vinote.smart.socket.protocol.p2p.client;

import java.util.concurrent.ArrayBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.exception.QueueOverflowStrategyException;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;

/**
 *
 * 业务消息处理线程
 *
 * @author Seer
 *
 */
class P2PClientProcessThread extends ProtocolProcessThread<BaseMessage> {
	private Logger logger = LogManager.getLogger(P2PClientProcessThread.class);
	private QueueOverflowStrategy strategy;
	private ArrayBlockingQueue<BaseMessage> list = new ArrayBlockingQueue<BaseMessage>(1024);

	public P2PClientProcessThread(String name, ProtocolDataProcessor<BaseMessage> processor,
		QueueOverflowStrategy strategy) {
		super(name, processor);
		this.strategy = strategy;
	}

	public void put(BaseMessage msg) {
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
				processor.process(null, msg);
			} catch (Exception e) {
				if (running) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}

}
