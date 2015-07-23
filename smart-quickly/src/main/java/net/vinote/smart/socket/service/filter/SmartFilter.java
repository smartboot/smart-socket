package net.vinote.smart.socket.service.filter;

import java.nio.ByteBuffer;
import java.util.List;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 *
 */
public interface SmartFilter {

	public void filterDataEntrys(TransportSession session, List<DataEntry> d);

	/**
	 * 消息处理前置预处理
	 *
	 * @param session
	 * @param d
	 */
	public void processFilter(Session session, DataEntry d);

	/**
	 * 消息接受前置预处理
	 *
	 * @param session
	 * @param d
	 */
	public void readFilter(TransportSession session, DataEntry d);

	/**
	 * 消息接受失败处理
	 */
	public void receiveFailHandler(TransportSession session, DataEntry d);

	public void writeFilter(TransportSession session, ByteBuffer d);
}
