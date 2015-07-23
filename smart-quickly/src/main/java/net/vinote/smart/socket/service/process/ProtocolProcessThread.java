package net.vinote.smart.socket.service.process;


/**
 * 
 * 协议消息处理线程抽象类
 * 
 * @author Seer
 *
 */
public abstract class ProtocolProcessThread extends Thread {
	/** 当前线程服务的处理器 */
	protected ProtocolDataProcessor processor;
	protected volatile boolean running = true;

	public ProtocolProcessThread(String name, ProtocolDataProcessor processor) {
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
