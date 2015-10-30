package net.vinote.smart.socket.protocol;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.transport.TransportSession;

/**
 *
 * 消息传输采用的协议 注意:同一个协议解析器切勿同时用于多个socket链路,否则将出息码流错乱情况
 *
 * @author Seer
 * @version Protocol.java, v 0.1 2015年3月13日 下午3:30:57 Seer Exp.
 */
public interface Protocol {
	/**
	 * 对于从Socket流中获取到的数据采用当前Protocol的实现类协议进行解析
	 *
	 * @param data
	 * @return 本次解码所成功解析的消息实例集合,不允许返回null
	 */
	public DataEntry decode(ByteBuffer data, TransportSession session);

	/**
	 * 封装畸形报文协议
	 *
	 * @return
	 */
	public DataEntry wrapInvalidProtocol(TransportSession session);

}
