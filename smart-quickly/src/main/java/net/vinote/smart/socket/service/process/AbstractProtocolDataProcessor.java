package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层协议消息处理器抽象类
 * 
 * @author Seer
 *
 */
public abstract class AbstractProtocolDataProcessor implements
		ProtocolDataProcessor {
	private QuicklyConfig quickConfig;

	public void init(QuicklyConfig config) throws Exception {
		this.quickConfig = config;
	}

	public final QuicklyConfig getQuicklyConfig() {
		return quickConfig;
	}

	@Override
	public Session getSession(TransportSession tsession) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean preReceive(TransportSession session, DataEntry entry) {
		// TODO Auto-generated method stub
		return true;
	}

}
