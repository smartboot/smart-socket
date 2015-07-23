package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;
import java.util.List;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 *
 */
public interface SmartFilterChain {

	public void doReadFilter(TransportSession session, List<DataEntry> dataList);

	public void doWriteFilter(TransportSession session, ByteBuffer buffer);
}
