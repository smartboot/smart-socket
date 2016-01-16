package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.lang.QuicklyConfig;

/**
 * 业务层协议消息处理器抽象类
 * 
 * @author Seer
 *
 */
public abstract class AbstractProtocolDataProcessor<T> implements ProtocolDataProcessor<T> {
	private QuicklyConfig<T> quickConfig;

	public void init(QuicklyConfig<T> config) {
		this.quickConfig = config;
	}

	public final QuicklyConfig<T> getQuicklyConfig() {
		return quickConfig;
	}
}
