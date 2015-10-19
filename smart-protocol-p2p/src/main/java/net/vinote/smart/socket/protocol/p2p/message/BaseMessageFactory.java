package net.vinote.smart.socket.protocol.p2p.message;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;

public class BaseMessageFactory implements ServiceMessageFactory{

	private Map<Integer, Class<? extends BaseMessage>> msgHaspMap = new HashMap<Integer, Class<? extends BaseMessage>>();

	private QuicklyConfig quicklyConfig; 

	public BaseMessageFactory(QuicklyConfig quicklyConfig) {
		this.quicklyConfig=quicklyConfig;
		quicklyConfig.setServiceMessageFactory(this);
	}

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
				RunLogger.getLogger().log(
					Level.WARNING,
					"MessageType=" + messageType + " has already regiested by " + msgHaspMap.get(messageType).getName()
					+ ", ingore " + msg.getName());
			} else {
				msgHaspMap.put(messageType, msg);
				RunLogger.getLogger().log(Level.SEVERE, "load Message Class[" + msg.getName() + "]");
			}
		} catch (Exception e) {
			RunLogger.getLogger().log(e);
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
			RunLogger.getLogger().log(Level.FINEST, "do you 吃饱了撑着啊,给我null");
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
				quicklyConfig.getServiceProcessorFactory().regist(p2pClass, processClass);
			}
			regiestMessage(p2pClass);
		}
	}

	public Class<?> getBaseMessage(int type) {
		return msgHaspMap.get(type);
	}
}
