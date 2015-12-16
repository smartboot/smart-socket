package net.vinote.smart.socket.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidParameterException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.transport.enums.ChannelServiceStatusEnum;

/**
 * @author Seer
 * @version NioQuickClient.java, v 0.1 2015年3月20日 下午2:55:08 Seer Exp.
 */
public class NioQuickClient extends AbstractChannelService {
	private Logger logger = LogManager.getLogger(NioQuickClient.class);
	/**
	 * Socket连接锁,用于监听连接超时
	 */
	private final Object conenctLock = new Object();

	/**
	 * 客户端会话信息
	 */
	private NioSession session;

	private SocketChannel socketChannel;

	/**
	 * @param config
	 */
	public NioQuickClient(final QuicklyConfig config) {
		super(config);
	}

	/**
	 * 接受并建立客户端与服务端的连接
	 *
	 * @param key
	 * @param selector
	 * @throws IOException
	 */
	@Override
	void acceptConnect(SelectionKey key, Selector selector) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		channel.finishConnect();
		key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
		// 自动修复链路
		if (session != null && config.isAutoRecover()) {
			session.initBaseChannelInfo(key);
			logger.info("Socket link has been recovered!");
		} else {
			session = new NioSession(key, config.getProtocolFactory().createProtocol(), config.getReceiver(),
				config.getFilters(), config.getCacheSize(),
				QueueOverflowStrategy.valueOf(config.getQueueOverflowStrategy()), config.isAutoRecover(),
				config.getDataBufferSize());
			logger.info("success connect to " + channel.socket().getRemoteSocketAddress().toString());
			config.getReceiver().initChannel(session);
			// ProtocolDataProcessor processor = config.getProcessor();
			// if (processor instanceof ClientProcessor) {
			// ((ClientProcessor) processor).createSession(session);
			// } else {
			// throw new InvalidClassException(
			// "Client Processor must implement interface " +
			// ClientProcessor.class.getName());
			// }
			synchronized (conenctLock) {
				conenctLock.notifyAll();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.vinote.smart.socket.transport.AbstractChannelService#
	 * exceptionInSelectionKey(java.nio.channels.SelectionKey,
	 * java.lang.Exception)
	 */
	@Override
	void exceptionInSelectionKey(final SelectionKey key, final Exception e) throws Exception {
		throw e;
	}

	@Override
	void exceptionInSelector(final Exception e) {
		logger.catching(e);
		if (ChannelServiceStatusEnum.RUNING == status && config.isAutoRecover()) {
			restart();
		} else {
			shutdown();
		}
	}

	@Override
	void readFromChannel(SelectionKey key) throws IOException {
		if (key.isReadable()) {
			SocketChannel channel = (SocketChannel) key.channel();
			ByteBuffer buffer = session.getReadBuffer();
			int readSize = 0;
			int loopTimes = READ_LOOP_TIMES;
			do {
				session.flushReadBuffer();
			} while ((key.interestOps() & SelectionKey.OP_READ) > 0 && (readSize = channel.read(buffer)) > 0
				&& --loopTimes > 0);// 读取管道中的数据块
			// 达到流末尾则注销读关注
			if (readSize == -1) {
				logger.info("the read channel[" + channel + "] has reached end-of-stream");
				key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
			}
		}
	}

	private void restart() {
		try {
			final Set<SelectionKey> keys = selector.keys();
			if (keys != null) {
				for (final SelectionKey key : keys) {
					key.cancel();
				}
			}
			if (socketChannel != null) {
				socketChannel.close();
			}
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
			socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort()));
			logger.info("Client " + config.getLocalIp() + " will reconnect to [IP:" + config.getHost() + " ,Port:"
				+ config.getPort() + "]");
		} catch (final IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.vinote.smart.socket.transport.ChannelService#shutdown()
	 */
	public final void shutdown() {
		updateServiceStatus(ChannelServiceStatusEnum.STOPPING);
		// config.getProcessor().shutdown();
		try {
			selector.close();
			selector.wakeup();
		} catch (final IOException e) {
			logger.warn(e.getMessage(), e);
		}
		try {
			socketChannel.close();
		} catch (final IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.vinote.smart.socket.transport.ChannelService#start()
	 */
	public final void start() {
		try {
			checkStart();
			assertAbnormalStatus();
			updateServiceStatus(ChannelServiceStatusEnum.STARTING);
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
			socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort()));
			serverThread = new Thread(this, "QuickClient-" + hashCode());
			serverThread.start();
			socketChannel.socket().setSoTimeout(config.getTimeout());

			if (session != null) {
				return;
			}
			synchronized (conenctLock) {
				if (session != null) {
					return;
				}
				try {
					conenctLock.wait(config.getTimeout());
				} catch (final InterruptedException e) {
					logger.warn("", e);
				}
			}

		} catch (final IOException e) {
			logger.warn("", e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.vinote.smart.socket.transport.AbstractChannelService#writeToChannel
	 * (java.nio.channels.SelectionKey, java.nio.channels.Selector)
	 */
	@Override
	void writeToChannel(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer;
		int loopTimes = WRITE_LOOP_TIMES;
		while ((buffer = session.getWriteBuffer()) != null && socketChannel.write(buffer) > 0 && --loopTimes > 0) {
			;
		}
	}

	@Override
	void checkStart() {
		super.checkStart();
		if (!config.isClient()) {
			throw new StatusException("invalid quciklyConfig");
		}
		if (StringUtils.isBlank(config.getHost())) {
			throw new InvalidParameterException("invalid host " + config.getHost());
		}
	}

}
