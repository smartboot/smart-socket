package net.vinote.smart.socket.service.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;

/**
 * 处理器管理<br/>
 * 维护各业务消息类型的处理器
 * 
 * @author Seer
 * @version ServiceProcessorManager.java, v 0.1 2015年3月13日 下午3:28:25 Seer Exp.
 */
public final class ServiceProcessorManager {
	private static ServiceProcessorManager instance = null;
	private Map<Class<? extends DataEntry>, AbstractServiceMessageProcessor> processorMap = new HashMap<Class<? extends DataEntry>, AbstractServiceMessageProcessor>();

	private ServiceProcessorManager() {
	}

	public static ServiceProcessorManager getInstance() {
		if (instance == null) {
			synchronized (ServiceProcessorManager.class) {
				if (instance == null) {
					instance = new ServiceProcessorManager();
				}
			}
		}
		return instance;
	}

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
		} catch (Exception e) {
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
