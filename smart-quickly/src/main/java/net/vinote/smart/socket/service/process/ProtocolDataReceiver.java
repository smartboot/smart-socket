package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 协议消息接受器
 * <p>
 * 从Socket中读取了一定量的数据流后,通过特定的协议解析器进行数据流的解码。 完成解码后数据流便被转换成特定的业务消息对象并调用该接口
 * {@link ProtocolDataReceiver} 实现类的receive进行接收。
 * </p>
 *
 * @author Seer
 * @version 1.0
 */
public interface ProtocolDataReceiver<T> {

	/**
	 * 接受业务消息
	 * <p>
	 * 传输层成功进行数据流解码后交由业务层接收该消息实体
	 * </p>
	 * <b>注:NIO实现中消息接受与发送处于同一线程中,因此若receive的实现类中存在阻塞情况,将导致数据发送同步阻塞</b>
	 *
	 * @see TransportSession#flushReadBuffer()
	 * @param session
	 *            本次消息的来源
	 * @param entry
	 *            待接收的消息
	 * @return
	 */
	public boolean receive(TransportSession<T> session, T entry);

	public Session<T> initSession(TransportSession<T> session);
}
