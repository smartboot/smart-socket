package net.vinote.smart.socket.transport.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.logging.Level;

import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.transport.ChannelService;
import net.vinote.smart.socket.transport.ChannelServiceStatus;

/**
 * @author Seer
 * @version AbstractChannelService.java, v 0.1 2015年3月19日 下午6:57:01 Seer Exp.
 */
abstract class AbstractChannelService implements ChannelService {
	/** 服务状态 */
	volatile ChannelServiceStatus status = ChannelServiceStatus.Init;

	/** 服务配置 */
	QuicklyConfig config;

	Selector selector;

	/**
	 * 传输层Channel服务处理线程
	 */
	Thread serverThread;

	/** 读管道单论操作读取次数 */
	final int READ_LOOP_TIMES;

	/** 写管道单论操作读取次数 */
	final int WRITE_LOOP_TIMES;

	public AbstractChannelService(final QuicklyConfig config) {
		this.config = config;
		READ_LOOP_TIMES = config.getReadLoopTimes();
		WRITE_LOOP_TIMES = config.getWriteLoopTimes();
		try {
			config.getProcessor().init(config);
		} catch (final Exception e) {
			status = ChannelServiceStatus.Abnormal;
			RunLogger.getLogger().log(Level.SEVERE, "", e);
		}
		RunLogger.getLogger().log(Level.SEVERE,
			"Registe MessageServer Processor[" + config.getProcessor().getClass().getName() + "] success");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public final void run() {
		updateServiceStatus(ChannelServiceStatus.RUNING);
		// 通过检查状态使之一直保持服务状态
		while (ChannelServiceStatus.RUNING == status) {
			try {
				// 此处会阻塞在selector.select()直至某个关注的事件将其唤醒
				while (selector.isOpen() && selector.select() > -1) {
					Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
					// 执行本次已触发待处理的事件
					while (keyIterator.hasNext()) {
						SelectionKey key = keyIterator.next();
						try {
							// 读取客户端数据
							if (key.isReadable()) {
								readFromChannel(key);
							} // 输出数据至客户端
							else if (key.isWritable()) {
								writeToChannel(key);
							} // 建立新连接,Client触发Connect,Server触发Accept
							else if (key.isAcceptable() || key.isConnectable()) {
								acceptConnect(key, selector);
							} else {
								System.out.println("奇怪了...");
							}
						} catch (Exception e) {
							exceptionInSelectionKey(key, e);
						} finally {
							// 移除已处理的事件
							keyIterator.remove();
						}
					}
				}

				if (!selector.isOpen()) {
					RunLogger.getLogger().log(Level.SEVERE, "Selector is already closed!");
					break;
				}

			} catch (Exception e) {
				exceptionInSelector(e);
			}
		}
		updateServiceStatus(ChannelServiceStatus.STOPPED);
		RunLogger.getLogger().log(Level.SEVERE, "Channel is stop!");
	}

	/**
	 * 接受并建立Socket连接
	 *
	 * @param key
	 * @param selector
	 * @throws IOException
	 */
	abstract void acceptConnect(SelectionKey key, Selector selector) throws IOException;

	/**
	 * 判断状态是否有异常
	 */
	final void assertAbnormalStatus() {
		if (status == ChannelServiceStatus.Abnormal) {
			throw new StatusException("channel service's status is abnormal");
		}
	}

	/**
	 * 处理某个已触发且发生了异常的SelectionKey
	 * 
	 * @param key
	 * @param e
	 * @throws Exception
	 */
	abstract void exceptionInSelectionKey(SelectionKey key, Exception e) throws Exception;

	/**
	 * 处理选择器层面的异常,此时基本上会导致当前的链路不再可用
	 * 
	 * @param e
	 */
	abstract void exceptionInSelector(Exception e);

	/**
	 * 从管道流中读取数据
	 * 
	 * @param key
	 * @param selector
	 * @throws IOException
	 */
	abstract void readFromChannel(SelectionKey key) throws IOException;

	/**
	 * 更新服务状态
	 * 
	 * @param status
	 */
	final void updateServiceStatus(final ChannelServiceStatus status) {
		this.status = status;
	}

	/**
	 * 往管道流中输出数据
	 * 
	 * @param key
	 * @param selector
	 * @throws IOException
	 */
	abstract void writeToChannel(SelectionKey key) throws IOException;
}
