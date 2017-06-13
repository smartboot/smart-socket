package net.vinote.smart.socket.service.process;

import java.util.concurrent.ArrayBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 *
 */
public abstract class AbstractServerDataProcessor<T> implements ProtocolDataProcessor<T> {
	private Logger logger = LogManager.getLogger(AbstractServerDataProcessor.class);
	public static final String SESSION_KEY = "SESSION";
	/** 消息处理线程 */
	private ServerDataProcessThread[] processThreads;
	/** 消息缓存队列 */
	private ArrayBlockingQueue<ProcessUnit> msgQueue;

	private QuicklyConfig<T> quickConfig;

	@SuppressWarnings("unchecked")
	@Override
	public void init(QuicklyConfig<T> config) {
		this.quickConfig = config;
		msgQueue = new ArrayBlockingQueue<ProcessUnit>(config.getThreadNum() * 500);
		// 启动线程池处理消息
		processThreads = new AbstractServerDataProcessor.ServerDataProcessThread[config.getThreadNum()];
		for (int i = 0; i < processThreads.length; i++) {
			processThreads[i] = new ServerDataProcessThread("ServerProcess-Thread-" + i);
			processThreads[i].setPriority(Thread.MAX_PRIORITY);
			processThreads[i].start();
		}
	}

	@Override
	public boolean receive(TransportSession<T> session, T entry) {
		// return msgQueue.offer(new ProcessUnit(session, entry));
		try {
			ProcessUnit unit=new ProcessUnit(session, entry);
			if(!msgQueue.offer(unit)) {
				msgQueue.put(unit);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void shutdown() {
		if (processThreads != null && processThreads.length > 0) {
			for (ServerDataProcessThread thread : processThreads) {
				thread.shutdown();
			}
		}
	}

	/**
	 * 消息数据元
	 * 
	 * @author zhengjunwei
	 *
	 */
	final class ProcessUnit {
		TransportSession<T> session;
		T msg;

		public ProcessUnit(TransportSession<T> session, T msg) {
			this.session = session;
			this.msg = msg;
		}
	}

	/**
	 * 服务端消息处理线程
	 * 
	 * @author zhengjunwei
	 *
	 */
	final class ServerDataProcessThread extends Thread {
		private volatile boolean running = true;

		public ServerDataProcessThread(String name) {
			super(name);
		}

		@Override
		public void run() {
			while (running) {
				try {
					ProcessUnit unit = AbstractServerDataProcessor.this.msgQueue.take();
					SmartFilter<T>[] filters = AbstractServerDataProcessor.this.quickConfig.getFilters();
					if (filters != null && filters.length > 0) {
						for (SmartFilter<T> h : filters) {
							h.processFilter(unit.session, unit.msg);
						}
					}
					Session<T> session = unit.session.getAttribute(SESSION_KEY);
					AbstractServerDataProcessor.this.process(session, unit.msg);
				} catch (Exception e) {
					if (running) {
						logger.warn(e.getMessage(), e);
					}
				}
			}
		}

		/**
		 * 停止消息处理线程
		 */
		public void shutdown() {
			running = false;
			this.interrupt();
		}
	}
}
