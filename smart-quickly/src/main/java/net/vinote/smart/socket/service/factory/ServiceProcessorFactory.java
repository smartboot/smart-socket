package net.vinote.smart.socket.service.factory;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;

/**
 * 业务消息处理器工厂
 * 
 * @author Seer
 *
 */
public interface ServiceProcessorFactory {

	/**
	 * 注册消息处理器
	 * 
	 * @param clazz
	 * @param process
	 */
	public <K extends DataEntry, V extends AbstractServiceMessageProcessor> void regist(Class<K> clazz,
			final Class<V> process);

	/**
	 * 获取处理器
	 * 
	 * @param clazz
	 * @return
	 */
	public AbstractServiceMessageProcessor getProcessor(Class<? extends DataEntry> clazz);

	/**
	 * 销毁处理器资源
	 */
	public void destory();

}
