package net.vinote.smart.socket.protocol;

/**
 *
 * 协议解析器创建工厂
 *
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
