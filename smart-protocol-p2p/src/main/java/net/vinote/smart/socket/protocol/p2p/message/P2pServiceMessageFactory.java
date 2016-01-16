package net.vinote.smart.socket.protocol.p2p.message;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.p2p.AbstractServiceMessageProcessor;

public class P2pServiceMessageFactory {
	private Logger logger = LogManager.getLogger(P2pServiceMessageFactory.class);
	private Map<Integer, Class<? extends BaseMessage>> msgHaspMap = new HashMap<Integer, Class<? extends BaseMessage>>();
	private Map<Class<? extends BaseMessage>, AbstractServiceMessageProcessor> processorMap = new HashMap<Class<? extends BaseMessage>, AbstractServiceMessageProcessor>();


	/**
	 * 注册消息
	 *
	 * @param msg
	 */
	private void regiestMessage(Class<? extends BaseMessage> msg) {
		try {
			Method m = msg.getMethod("getMessageType");
			Integer messageType = (Integer) m.invoke(msg.newInstance());
			if (msgHaspMap.containsKey(messageType)) {
				logger.warn("MessageType=" + messageType + " has already regiested by "
					+ msgHaspMap.get(messageType).getName() + ", ingore " + msg.getName());
			} else {
				msgHaspMap.put(messageType, msg);
				logger.info("load Message Class[" + msg.getName() + "]");
			}
		} catch (Exception e) {
			logger.warn("", e);
		}
	}

	/**
	 * 加载属性文件所配置的消息以及处理器<br/>
	 * key:消息类型 value：消息处理器(可以为空)
	 *
	 * @param properties
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void loadFromProperties(Properties properties) throws ClassNotFoundException {
		if (properties == null) {
			logger.debug("do you 吃饱了撑着啊,给我null");
			return;
		}
		Enumeration keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement().toString().trim();
			Class<? extends BaseMessage> p2pClass = null;
			p2pClass = (Class<? extends BaseMessage>) Class.forName(key);

			String processClazz = properties.getProperty(key);
			if (!StringUtils.isBlank(processClazz)) {
				Class<? extends AbstractServiceMessageProcessor> processClass = (Class<? extends AbstractServiceMessageProcessor>) Class
					.forName(processClazz);
				regist(p2pClass, processClass);
			}
			regiestMessage(p2pClass);
		}
	}

	/**
	 * 注册消息处理器
	 *
	 * @param clazz
	 * @param process
	 */
	private <K extends BaseMessage, V extends AbstractServiceMessageProcessor> void regist(Class<K> clazz,
		final Class<V> process) {
		try {
			AbstractServiceMessageProcessor processor = process.newInstance();
			processor.init();
			processorMap.put(clazz, processor);
			logger.info("load Service Processor Class[" + process.getName() + "] for " + clazz.getName());
		} catch (InstantiationException e) {
			logger.warn("", e);
		} catch (IllegalAccessException e) {
			logger.warn("", e);
		}
	}

	public Class<?> getBaseMessage(int type) {
		return msgHaspMap.get(type);
	}

	// @Override
	public void destory() {
		for (AbstractServiceMessageProcessor processor : processorMap.values()) {
			processor.destory();
		}
		processorMap.clear();
		msgHaspMap.clear();
	}

	// @Override
	public AbstractServiceMessageProcessor getProcessor(Class<? extends BaseMessage> clazz) {
		return processorMap.get(clazz);
	}
}
