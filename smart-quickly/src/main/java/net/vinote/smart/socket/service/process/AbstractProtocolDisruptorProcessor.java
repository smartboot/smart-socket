package net.vinote.smart.socket.service.process;

import java.util.concurrent.Executors;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.service.session.SessionManager;
import net.vinote.smart.socket.transport.TransportSession;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * 基于Disruptor实现的业务层协议消息处理器抽象类
 *
 * @author Seer
 *
 */
public abstract class AbstractProtocolDisruptorProcessor implements
		ProtocolDataProcessor {
	protected class ProcessUnit {
		public String sessionId;
		public DataEntry msg;
	}

	private class ProcessUnitEventHandler implements EventHandler<ProcessUnit> {
		public void onEvent(ProcessUnit unit, long sequence, boolean endOfBatch) {
			SmartFilter[] handlers = getQuicklyConfig().getFilters();
			try {
				if (handlers != null && handlers.length > 0) {
					for (SmartFilter h : handlers) {
						h.processFilter(SessionManager.getInstance()
								.getSession(unit.sessionId), unit.msg);
					}
				}
				process(unit);
			} catch (Exception e) {
				RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
			}

		}
	}

	private class ProcessUnitFactory implements EventFactory<ProcessUnit> {
		public ProcessUnit newInstance() {
			return new ProcessUnit();
		}
	}

	private QuicklyConfig quickConfig;

	private RingBuffer<ProcessUnit> ringBuffer;

	private Disruptor<ProcessUnit> disruptor;

	public final QuicklyConfig getQuicklyConfig() {
		return quickConfig;
	}

	@SuppressWarnings("unchecked")
	public void init(QuicklyConfig config) throws Exception {
		quickConfig = config;
		disruptor = new Disruptor<ProcessUnit>(new ProcessUnitFactory(),
				quickConfig.getCacheSize(), Executors.newCachedThreadPool());
		disruptor.handleEventsWith(new ProcessUnitEventHandler());
		ringBuffer = disruptor.start();
	}

	@Override
	public boolean preReceive(TransportSession tsession, DataEntry msg) {
		// 会话封装并分配处理线程
		Session session = getSession(tsession);
		return !session.notifySyncMessage(msg);
	}

	public final boolean receive(TransportSession tsession, DataEntry msg) {
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			ProcessUnit event = ringBuffer.get(sequence);
			event.sessionId = getSession(tsession).getId();
			event.msg = msg;
		} finally {
			ringBuffer.publish(sequence);
		}
		return true;
	}

	@Override
	public void shutdown() {
		disruptor.shutdown();
	}
}
