package net.vinote.smart.socket.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.transport.enums.ChannelServiceStatusEnum;
import net.vinote.smart.socket.transport.enums.SessionStatusEnum;

/**
 * NIO服务器
 *
 * @author Seer
 *
 */
public final class NioQuickServer<T> extends AbstractChannelService<T> {
	private Logger logger = LogManager.getLogger(NioQuickServer.class);
	private ServerSocketChannel server;

	public NioQuickServer(final QuicklyConfig<T> config) {
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
	protected void acceptConnect(final SelectionKey key, final Selector selector) throws IOException {

		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);
		SelectionKey socketKey = socketChannel.register(selector, SelectionKey.OP_READ);
		NioSession<T> session = new NioSession<T>(socketKey, config);
		socketKey.attach(new NioAttachment(session));
		socketChannel.finishConnect();
		config.getProcessor().initChannel(session);
	}

	@Override
	protected void exceptionInSelectionKey(SelectionKey key, final Exception e) throws Exception {
		logger.warn("Close Channel because of Exception", e);
		final Object att = key.attach(null);
		if (att instanceof NioAttachment) {
			((NioAttachment) att).getSession().close();
		}
		key.channel().close();
		logger.info("close connection " + key.channel());
		key.cancel();
	}

	@Override
	protected void exceptionInSelector(Exception e) {
		logger.warn(e.getMessage(), e);
	}

	@Override
	protected void readFromChannel(SelectionKey key, NioAttachment attach) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		NioSession<?> session = attach.getSession();
		int readSize = 0;
		int loopTimes = READ_LOOP_TIMES;// 轮训次数,以便及时让出资源
		while ((readSize = socketChannel.read(session.flushReadBuffer())) > 0 && --loopTimes > 0)
			;// 读取管道中的数据块
		// 达到流末尾则注销读关注
		if (readSize == -1 || session.getStatus() == SessionStatusEnum.CLOSING) {
			session.cancelReadAttention();
			// key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
			if (session.getWriteBuffer() == null || key.isValid()) {
				session.close();
				logger.info("关闭客户端[" + socketChannel + "]");
			} else {
				logger.info("注销客户端[" + socketChannel + "]读关注");
			}
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
			logger.warn("", e1);
		}
		try {
			server.close();
		} catch (final IOException e) {
			logger.warn("", e);
		}
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
				address = new InetSocketAddress(config.getLocalIp(), config.getPort());
			}
			server.socket().bind(address);
			selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
			serverThread = new Thread(this, "Nio-Server");
			serverThread.start();
		} catch (final IOException e) {
			logger.catching(e);
			shutdown();
			throw e;
		}
	}

	@Override
	protected void writeToChannel(SelectionKey key, NioAttachment attach) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		NioSession<?> session = attach.getSession();
		ByteBuffer buffer;
		int loopTimes = WRITE_LOOP_TIMES;// 轮训次数,一遍及时让出资源
		// buffer = session.getByteBuffer()若读取不到数据,则内部会移除写关注
		// socketChannel.write(buffer) == 0则表示当前以不可写
		while ((buffer = session.getWriteBuffer()) != null && socketChannel.write(buffer) > 0 && --loopTimes > 0) {
			;
		}
		if (session.getStatus() == SessionStatusEnum.CLOSING && (buffer = session.getWriteBuffer()) == null) {
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
			logger.info("Running with " + config.getPort() + " port");
			config.getProcessor().init(config);
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
