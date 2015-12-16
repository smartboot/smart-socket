package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 *
 */
public interface SmartFilterChain<T> {

	public void doReadFilter(TransportSession<T> session, T buffer);

	public void doWriteFilter(TransportSession<T> session, ByteBuffer buffer);
}
