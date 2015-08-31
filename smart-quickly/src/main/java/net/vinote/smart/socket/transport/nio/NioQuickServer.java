package net.vinote.smart.socket.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.extension.cluster.Client2ClusterMessageProcessor;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.transport.enums.ChannelServiceStatusEnum;
import net.vinote.smart.socket.transport.enums.SessionStatusEnum;
import net.vinote.smart.socket.transport.filter.TransportFilter;
import net.vinote.smart.socket.transport.filter.TransportFilterChain;
import net.vinote.smart.socket.transport.filter.impl.TansportFilterChainImpl;
import net.vinote.smart.socket.transport.filter.impl.TransportAliveFilter;

/**
 * NIO服务器
 *
 * @author Seer
 *
 */
public final class NioQuickServer extends AbstractChannelService {
	private ServerSocketChannel server;

	/** 传输层过滤器 */
	private TransportFilterChain filterChain = null;

	public NioQuickServer(final QuicklyConfig config) {
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
	protected void acceptConnect(final SelectionKey key, final Selector selector)
			throws IOException {

		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);
		SelectionKey socketKey = socketChannel.register(selector,
				SelectionKey.OP_READ);
		NioSession session = null;
		// 判断当前链路的消息是否交由集群服务器处理
		if (config.getClusterTriggerStrategy() != null
				&& config.getClusterTriggerStrategy().cluster()) {
			session = new NioSession(socketKey, config,
					Client2ClusterMessageProcessor.getInstance());
		} else {
			session = new NioSession(socketKey, config);
		}
		socketKey.attach(session);
		socketChannel.finishConnect();
		filterChain.doAcceptFilter(session);// 执行过滤器
	}

	@Override
	protected void exceptionInSelectionKey(SelectionKey key, final Exception e)
			throws Exception {
		RunLogger.getLogger().log(Level.WARNING,
				"Close Channel because of Exception", e);
		final Object att = key.attach(null);
		if (att instanceof NioSession) {
			((NioSession) att).close();
		}
		key.channel().close();
		RunLogger.getLogger().log(Level.SEVERE,
				"close connection " + key.channel());
		key.cancel();
	}

	@Override
	protected void exceptionInSelector(Exception e) {
		RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
	}

	@Override
	protected void readFromChannel(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		NioSession session = (NioSession) key.attachment();
		ByteBuffer buffer = session.getReadBuffer();
		int readSize = 0;
		int loopTimes = READ_LOOP_TIMES;// 轮训次数,以便及时让出资源
		do {
			session.flushReadBuffer();
		} while (key.isValid()
				&& (key.interestOps() & SelectionKey.OP_READ) > 0
				&& --loopTimes > 0
				&& (readSize = socketChannel.read(buffer)) > 0);// 读取管道中的数据块
		// 达到流末尾则注销读关注
		if (readSize == -1 || session.getStatus() == SessionStatusEnum.CLOSING) {
			RunLogger.getLogger().log(Level.SEVERE,
					"注销客户端[" + socketChannel + "]读关注");
			key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
		}
	}

	public void shutdown() {
		updateServiceStatus(ChannelServiceStatusEnum.STOPPING);
		config.getProcessor().shutdown();
		try {
			if (selector != null) {
				selector.close();
				selector.wakeup();
			}
		} catch (final IOException e1) {
			RunLogger.getLogger().log(e1);
		}
		try {
			server.close();
		} catch (final IOException e) {
			RunLogger.getLogger().log(e);
		}
		Client2ClusterMessageProcessor.getInstance().shutdown();
	}

	public void start() throws IOException {
		try {
			checkStart();
			assertAbnormalStatus();
			updateServiceStatus(ChannelServiceStatusEnum.STARTING);
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			InetSocketAddress address = null;
			if (StringUtils.isBlank(config.getLocalIp())) {
				address = new InetSocketAddress(config.getPort());
			} else {
				address = new InetSocketAddress(config.getLocalIp(),
						config.getPort());
			}
			server.socket().bind(address);
			selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT, config);
			serverThread = new Thread(this, "Nio-Server");
			serverThread.start();
			if (config.getClusterUrl() != null) {
				// 启动集群服务
				try {
					Client2ClusterMessageProcessor.getInstance().init(config);
					RunLogger.getLogger().log(Level.SEVERE,
							"Start Cluster Service...");
				} catch (final Exception e) {
					RunLogger.getLogger().log(Level.WARNING, "", e);
				}
			}
		} catch (final IOException e) {
			shutdown();
			throw e;
		}
	}

	@Override
	protected void writeToChannel(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		NioSession session = (NioSession) key.attachment();
		ByteBuffer buffer;
		int loopTimes = WRITE_LOOP_TIMES;// 轮训次数,一遍及时让出资源
		// buffer = session.getByteBuffer()若读取不到数据,则内部会移除写关注
		// socketChannel.write(buffer) == 0则表示当前以不可写
		while ((buffer = session.getWriteBuffer()) != null
				&& socketChannel.write(buffer) > 0 && --loopTimes > 0) {
			;
		}
		if (session.getStatus() == SessionStatusEnum.CLOSING
				&& (buffer = session.getWriteBuffer()) == null) {
			session.close();
		}
	}

	@Override
	protected void notifyWhenUpdateStatus(ChannelServiceStatusEnum status) {
		if (status == null) {
			return;
		}
		switch (status) {
		case RUNING:
			RunLogger.getLogger().log(Level.SEVERE,
					"Running with " + config.getPort() + " port");
			// 启动过滤器
			filterChain = new TansportFilterChainImpl(
					new TransportFilter[] { new TransportAliveFilter() });
			break;

		default:
			break;
		}
	}

	@Override
	void checkStart() {
		super.checkStart();
		if (!config.isServer()) {
			throw new StatusException("invalid quciklyConfig");
		}
	}

}
