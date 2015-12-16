package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层协议消息处理器抽象类
 * 
 * @author Seer
 *
 */
public abstract class AbstractProtocolDataProcessor<T> implements ProtocolDataProcessor<T> {
	private QuicklyConfig quickConfig;

	public void init(QuicklyConfig config) throws Exception {
		this.quickConfig = config;
	}

	public final QuicklyConfig getQuicklyConfig() {
		return quickConfig;
	}

	@Override
	public void initSession(TransportSession<T> tsession) {
		throw new UnsupportedOperationException();
	}

	@Override
	public  boolean preReceive(TransportSession<T> session, T entry) {
		return true;
	}

}
