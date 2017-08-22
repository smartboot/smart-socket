package org.smartboot.socket.protocol;

/**
 *
 * 协议解析器创建工厂.
 * 每一个Socket链路都会通过ProtocolFactory构建出一个对应的Protocol对象
 * @author Administrator
 *
 */
public interface ProtocolFactory<T> {
	/**
	 * 创建协议解析器
	 * 
	 * @return
	 */
	public Protocol<T> createProtocol();
}
