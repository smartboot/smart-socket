package net.vinote.smart.socket.service.process;

/**
 * 
 * 协议消息处理线程抽象类
 * 
 * @author Seer
 * @version ProtocolProcessThread.java, v 0.1 2015年8月24日 下午2:21:57 Seer Exp.
 */
public abstract class ProtocolProcessThread<T> extends Thread {
	/** 当前线程服务的处理器 */
	protected final ProtocolDataProcessor<T> processor;
	protected volatile boolean running = true;

	public ProtocolProcessThread(String name, ProtocolDataProcessor<T> processor) {
		super(name);
		this.processor = processor;
	}

	/**
	 * 停止消息处理线程
	 */
	public void shutdown() {
		running = false;
		this.interrupt();
	}

	public boolean isRunning() {
		return running;
	}
}
