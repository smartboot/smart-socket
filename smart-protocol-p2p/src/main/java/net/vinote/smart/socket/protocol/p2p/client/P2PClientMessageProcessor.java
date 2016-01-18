package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.Session;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.transport.TransportSession;

public class P2PClientMessageProcessor extends AbstractProtocolDataProcessor<BaseMessage> {
	private P2PClientProcessThread processThread;
	private Session session;

	@Override
	public void init(QuicklyConfig<BaseMessage> config) {
		super.init(config);
		processThread = new P2PClientProcessThread("P2PClient-Thread", this,
			QueueOverflowStrategy.valueOf(getQuicklyConfig().getQueueOverflowStrategy()));
		processThread.start();
	}

	public void shutdown() {
		processThread.shutdown();
	}

	@Override
	public void process(TransportSession<BaseMessage> session, BaseMessage msg) throws Exception {
		//System.out.println(msg);
	}

	@Override
	public boolean receive(TransportSession<BaseMessage> session, BaseMessage entry) {
		if (!this.session.notifySyncMessage(entry)) {
			// 同步响应消息若出现超时情况,也会进到if里面
			processThread.put(entry);
		}
		return true;
	}

	@Override
	public void initChannel(TransportSession<BaseMessage> session) {
		this.session = new P2PSession(session);
	}

	/**
	 * Getter method for property <tt>sesson</tt>.
	 *
	 * @return property value of sesson
	 */
	public final Session getSession() {
		return session;
	}

}
