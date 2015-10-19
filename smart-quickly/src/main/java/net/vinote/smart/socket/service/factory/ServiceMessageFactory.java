package net.vinote.smart.socket.service.factory;

import java.util.Properties;

public interface ServiceMessageFactory {


	/**
	 * 加载属性文件所配置的消息以及处理器<br/>
	 * key:消息类型 value：消息处理器(可以为空)
	 *
	 * @param properties
	 * @throws ClassNotFoundException
	 */
	public void loadFromProperties(Properties properties) throws ClassNotFoundException ;


}
