package net.vinote.smart.socket.service.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务层会话管理器
 *
 * @author Seer
 *
 */
public final class SessionManager {

	private static SessionManager instance = null;
	private Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();

	private AtomicLong lock = new AtomicLong(0);

	/*
	 * 高优先级线程用于回收失效会话资源
	 */
	private static class SessionCollectionHandler extends Thread {
		private Logger logger = LoggerFactory.getLogger(SessionCollectionHandler.class);

		SessionCollectionHandler(ThreadGroup g, String name) {
			super(g, name);
		}

		@Override
		public void run() {
			Map<String, Session> sessionMap = SessionManager.getInstance().sessionMap;
			for (;;) {
				long nextCollectTime = Long.MAX_VALUE;
				long curTime = System.currentTimeMillis();
				for (String key : sessionMap.keySet()) {
					Session session = sessionMap.get(key);
					if (session.getMaxInactiveInterval() <= 0) {// 用不失效的会话
						continue;
					}
					// 剩余闲置时长
					long remainTime = session.getMaxInactiveInterval() - (curTime - session.getLastAccessedTime());
					// 超时失效该会话
					if (remainTime <= 0 || session.isInvalid()) {
						if (!session.isInvalid()) {
							session.invalidate();
						}
						Session s = sessionMap.remove(key);
						logger.info("Release Overtime Session" + s);
					} else {
						// 计算下一次执行回收的间隔时间
						nextCollectTime = nextCollectTime > remainTime ? remainTime : nextCollectTime;
					}
				}
				synchronized (SessionManager.instance.lock) {
					try {
						logger.info(nextCollectTime + "ms later will be collecting session resource!");
						SessionManager.instance.lock.set(nextCollectTime);
						SessionManager.instance.lock.wait(nextCollectTime);
					} catch (InterruptedException e) {
						logger.warn("", e);
					}
				}
			}
		}
	}

	static {
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		for (ThreadGroup tgn = tg; tgn != null; tg = tgn, tgn = tg.getParent()) {
			;
		}
		Thread handler = new SessionCollectionHandler(tg, "QuickSession Collection Handler");
		handler.setPriority(Thread.MAX_PRIORITY);
		handler.setDaemon(true);
		handler.start();
	}

	private SessionManager() {
	}

	public static SessionManager getInstance() {
		if (instance == null) {
			synchronized (SessionManager.class) {
				if (instance == null) {
					instance = new SessionManager();
				}
			}
		}
		return instance;
	}

	/**
	 * 获取会话
	 *
	 * @param sessionID
	 * @return
	 */
	public Session getSession(String sessionID) {
		return sessionMap.get(sessionID);
	}

	/**
	 *
	 * @param session
	 */
	public void registSession(Session session) {
		sessionMap.put(session.getId(), session);
		// 遇到一个存活时长更短的会话则激活一次回收器
		if (session.getMaxInactiveInterval() > 0 && session.getMaxInactiveInterval() < lock.get()) {
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
}
