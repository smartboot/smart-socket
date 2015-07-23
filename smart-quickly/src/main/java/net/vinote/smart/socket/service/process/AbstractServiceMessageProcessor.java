package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.session.Session;

/**
 * 应用业务层消息处理器
 * 
 * @author Seer
 *
 */
public abstract class AbstractServiceMessageProcessor {
	/**
	 * 初始化处理器,整个生命周期内只在被构造时被调用
	 */
	public void init() {
	}

	public abstract void processor(Session session, DataEntry message)
			throws Exception;

	/**
	 * 处理集群消息
	 * 
	 * @param session
	 * @param message
	 * @return 响应消息
	 */
	public DataEntry processCluster(Session session, DataEntry message) {
		throw new UnsupportedOperationException(this.getClass().getName()
				+ " unsupport to operate cluster message!");
	};

	/**
	 * 处理器销毁时掉用该方法用以资源回收
	 */
	public void destory() {
	}
}
