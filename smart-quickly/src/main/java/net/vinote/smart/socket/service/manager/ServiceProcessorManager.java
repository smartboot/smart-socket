package net.vinote.smart.socket.service.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.factory.ServiceProcessorFactory;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;

/**
 * 处理器管理<br/>
 * 维护各业务消息类型的处理器
 * 
 * @author Seer
 * @version ServiceProcessorManager.java, v 0.1 2015年3月13日 下午3:28:25 Seer Exp.
 */
public final class ServiceProcessorManager implements ServiceProcessorFactory{
	private Map<Class<? extends DataEntry>, AbstractServiceMessageProcessor> processorMap = new HashMap<Class<? extends DataEntry>, AbstractServiceMessageProcessor>();

	/**
	 * 注册消息处理器
	 * 
	 * @param clazz
	 * @param process
	 */
	public <K extends DataEntry, V extends AbstractServiceMessageProcessor> void regist(
			Class<K> clazz, final Class<V> process) {
		try {
			AbstractServiceMessageProcessor processor = process.newInstance();
			processor.init();
			processorMap.put(clazz, processor);
			RunLogger.getLogger().log(
					Level.SEVERE,
					"load Service Processor Class[" + process.getName()
							+ "] for " + clazz.getName());
		} catch (InstantiationException e) {
			RunLogger.getLogger().log(e);
		} catch (IllegalAccessException e) {
			RunLogger.getLogger().log(e);
		}
	}

	/**
	 * 获取处理器
	 * 
	 * @param clazz
	 * @return
	 */
	public AbstractServiceMessageProcessor getProcessor(
			Class<? extends DataEntry> clazz) {
		return processorMap.get(clazz);
	}

	/**
	 * 销毁处理器资源
	 */
	public void destory() {
		for (AbstractServiceMessageProcessor processor : processorMap.values()) {
			processor.destory();
		}
		processorMap.clear();
	}
}
