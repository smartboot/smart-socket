package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 *
 */
public interface SmartFilterChain {

	public void doReadFilter(TransportSession session, ByteBuffer buffer);

	public void doWriteFilter(TransportSession session, ByteBuffer buffer);
}
