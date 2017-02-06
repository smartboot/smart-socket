package net.vinote.smart.socket.protocol.p2p.server;

import java.util.concurrent.ArrayBlockingQueue;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.MessageHandler;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 *
 */
public final class P2PServerMessageProcessor extends AbstractProtocolDataProcessor<BaseMessage> {
	private static final String SESSION_KEY = "SESSION";
	private P2pServiceMessageFactory serviceMessageFactory;

	public P2PServerMessageProcessor(P2pServiceMessageFactory serviceMessageFactory) {
		this.serviceMessageFactory = serviceMessageFactory;
	}

	/** 消息处理线程 */
	private ProtocolProcessThread<BaseMessage>[] processThreads;
	/** 消息缓存队列 */
	private ArrayBlockingQueue<ProcessUnit> msgQueue;

	private int msgQueueMaxSize;

	@Override
	public void init(QuicklyConfig<BaseMessage> config) {
		super.init(config);
		msgQueueMaxSize = config.getThreadNum() * 500;
		msgQueue = new ArrayBlockingQueue<ProcessUnit>(msgQueueMaxSize);
		// 启动线程池处理消息
		processThreads = new P2PServerProcessThread[config.getThreadNum()];
		for (int i = 0; i < processThreads.length; i++) {
			processThreads[i] = new P2PServerProcessThread("OMC-Server-Process-Thread-" + i, this, msgQueue);
			processThreads[i].setPriority(Thread.MAX_PRIORITY);
			processThreads[i].start();
		}
	}

	public void shutdown() {
		if (processThreads != null && processThreads.length > 0) {
			for (ProtocolProcessThread<BaseMessage> thread : processThreads) {
				thread.shutdown();
			}
		}
	}

	@Override
	public void process(TransportSession<BaseMessage> tsession, BaseMessage entry) throws Exception {
		P2PSession session = tsession.getAttribute(SESSION_KEY);
		SmartFilter<BaseMessage>[] filters = getQuicklyConfig().getFilters();
		if (filters != null && filters.length > 0) {
			for (SmartFilter<BaseMessage> h : filters) {
				h.processFilter(tsession, entry);
			}
		}
		MessageHandler handler = serviceMessageFactory.getProcessor(entry.getClass());
		handler.handler(session, entry);
	}

	@Override
	public boolean receive(TransportSession<BaseMessage> session, BaseMessage entry) {
		return msgQueue.offer(new ProcessUnit(session, entry));
//		try {
//			msgQueue.put(new ProcessUnit(session, entry));
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		 return true;
	}

	@Override
	public void initChannel(TransportSession<BaseMessage> session) {
		session.setAttribute(SESSION_KEY, new P2PSession(session));
	}
}
