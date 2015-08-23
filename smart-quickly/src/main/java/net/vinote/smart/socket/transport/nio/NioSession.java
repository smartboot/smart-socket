package net.vinote.smart.socket.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

import net.vinote.smart.socket.exception.CacheFullException;
import net.vinote.smart.socket.exception.NotYetReconnectedException;
import net.vinote.smart.socket.exception.QueueOverflowStrategyException;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.filter.impl.SmartFilterChainImpl;
import net.vinote.smart.socket.service.process.ProtocolDataReceiver;
import net.vinote.smart.socket.transport.TransportSession;
import net.vinote.smart.socket.transport.enums.SessionStatusEnum;

/**
 * 维护客户端-》服务端 或 服务端-》客户端 的当前会话
 *
 * @author Administrator
 *
 */
public class NioSession extends TransportSession {
	private static final RunLogger logger = RunLogger.getLogger();
	private SelectionKey channelKey = null;

	/** 响应消息缓存队列 */
	private ArrayBlockingQueue<byte[]> writeCacheList;

	private ByteBuffer writeBuffer;

	private Object writeLock = new Object();

	private String remoteIp;
	private String remoteHost;
	private int remotePort;

	private String localAddress;

	/** 是否已注销读关注 */
	private boolean readClosed = false;

	/**
	 * @param channel
	 *            当前的Socket管道
	 * @param protocol
	 *            当前channel数据流采用的解析协议
	 * @param processor
	 *            当前channel消息的处理器
	 */
	public NioSession(SelectionKey channelKey, QuicklyConfig config) {
		initBaseChannelInfo(channelKey);
		super.quickConfig = config;
		super.protocol = config.getProtocolFactory().createProtocol();
		super.chain = new SmartFilterChainImpl(config.getProcessor(), config.getFilters());
		writeCacheList = new ArrayBlockingQueue<byte[]>(config.getCacheSize());
	}

	public NioSession(SelectionKey channelKey, QuicklyConfig config, ProtocolDataReceiver receiver) {
		this(channelKey, config);
		super.chain = new SmartFilterChainImpl(receiver, config.getFilters());
	}

	@Override
	protected void cancelReadAttention() {
		readClosed = true;
		channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_READ);
	}

	@Override
	public void close(boolean immediate) {
		super.close(immediate || writeCacheList.isEmpty());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.zjw.platform.quickly.TransportSession#close()
	 */
	@Override
	protected void close0() {
		if (getStatus() == SessionStatusEnum.Closed) {
			return;
		}
		writeCacheList.clear();
		// protocol = null;
		try {
			channelKey.channel().close();
			logger.log(Level.SEVERE, "close connection " + channelKey.channel());
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
		channelKey.cancel();
		channelKey.selector().wakeup();// 必须唤醒一次选择器以便移除该Key,否则端口会处于CLOSE_WAIT状态
		/*
		 * logger.log(Level.SEVERE, "close dump", new
		 * Throwable().fillInStackTrace());
		 */
	}

	@Override
	public String getLocalAddress() {
		return localAddress;
	}

	@Override
	public String getRemoteAddr() {
		return remoteIp;
	}

	@Override
	public String getRemoteHost() {
		return remoteHost;
	}

	@Override
	public int getRemotePort() {
		return remotePort;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.zjw.platform.quickly.Session#cancelReadAttention()
	 */

	public ByteBuffer getWriteBuffer() {
		if (writeBuffer == null || !writeBuffer.hasRemaining()) {
			byte[] array = writeCacheList.poll();

			if (array != null) {
				writeBuffer = ByteBuffer.wrap(array);
				chain.doWriteFilter(this, writeBuffer);
			} else {
				writeBuffer = null;
				// 不具备写条件,移除该关注
				if (writeCacheList.size() == 0) {
					synchronized (writeLock) {
						if (writeCacheList.size() == 0) {
							channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_WRITE);
						}
					}
					resumeReadAttention();
				}
			}
		}
		return writeBuffer;
	}

	void initBaseChannelInfo(SelectionKey channelKey) {
		Socket socket = ((SocketChannel) channelKey.channel()).socket();
		InetSocketAddress remoteAddr = (InetSocketAddress) socket.getRemoteSocketAddress();
		remoteIp = remoteAddr.getAddress().getHostAddress();
		localAddress = socket.getLocalAddress().getHostAddress();
		remotePort = remoteAddr.getPort();
		remoteHost = remoteAddr.getHostName();
		this.channelKey = channelKey;
	}

	@Override
	public boolean isValid() {
		return channelKey.isValid();
	}

	@Override
	public void pauseReadAttention() {
		if ((channelKey.interestOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
			channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_READ);
		}
	}

	@Override
	public void resumeReadAttention() {
		if (readClosed) {
			return;
		}
		if ((channelKey.interestOps() & SelectionKey.OP_READ) != SelectionKey.OP_READ) {
			channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_READ);
			// RunLogger.getLogger().log(Level.FINE, "释放读流控" +
			// writeCacheList.size());
		}
	}

	@Override
	public String toString() {
		return "Session [channel=" + channelKey.channel() + ", protocol=" + protocol + ", receiver="
			+ getQuickConfig().getProcessor() + ", getClass()=" + getClass() + ", hashCode()=" + hashCode()
			+ ", toString()=" + super.toString() + "]";
	}

	@Override
	public void write(byte[] writeData) throws IOException, CacheFullException {
		try {
			switch (QueueOverflowStrategy.valueOf(quickConfig.getQueueOverflowStrategy())) {
			case DISCARD:
				if (!writeCacheList.offer(writeData)) {
					throw new CacheFullException("cache is full now");
				}
				break;
			case WAIT:
				try {
					writeCacheList.put(writeData);
				} catch (InterruptedException e) {
					RunLogger.getLogger().log(e);
				}
				break;
			default:
				throw new QueueOverflowStrategyException("Invalid overflow strategy "
					+ quickConfig.getQueueOverflowStrategy());
			}

		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		} finally {
			if (!channelKey.isValid()) {
				if (getQuickConfig().isAutoRecover()) {
					throw new NotYetReconnectedException("Network anomaly, will reconnect");
				} else {
					writeCacheList.clear();
					throw new IOException("Channel is invalid now!");
				}
			} else {
				synchronized (writeLock) {
					channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_WRITE);
					channelKey.selector().wakeup();
				}
			}
		}

	}

	/*
	 * 将数据输出至缓存,若缓存已满则返回false (non-Javadoc)
	 *
	 * @see com.zjw.platform.quickly.Session#write(byte[])
	 */
	@Override
	public void write(DataEntry data) throws IOException, CacheFullException {
		write(data.encode());
	}

}
