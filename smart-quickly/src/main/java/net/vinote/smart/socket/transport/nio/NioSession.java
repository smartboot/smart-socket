package net.vinote.smart.socket.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.exception.CacheFullException;
import net.vinote.smart.socket.exception.NotYetReconnectedException;
import net.vinote.smart.socket.exception.QueueOverflowStrategyException;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.protocol.Protocol;
import net.vinote.smart.socket.service.filter.SmartFilter;
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
public class NioSession<T> extends TransportSession<T> {
	private Logger logger = LogManager.getLogger(NioSession.class);
	private SelectionKey channelKey = null;

	/** 响应消息缓存队列 */
	private ArrayBlockingQueue<ByteBuffer> writeCacheQueue;

	private ByteBuffer writeBuffer;

	private Object writeLock = new Object();

	private String remoteIp;
	private String remoteHost;
	private int remotePort;

	private String localAddress;

	/** 是否已注销读关注 */
	private boolean readClosed = false;

	private QueueOverflowStrategy strategy = QueueOverflowStrategy.DISCARD;
	/** 是否自动修复链路 */
	private boolean autoRecover;

	/**
	 * @param channel
	 *            当前的Socket管道
	 * @param protocol
	 *            当前channel数据流采用的解析协议
	 * @param processor
	 *            当前channel消息的处理器
	 */
	public NioSession(SelectionKey channelKey, final Protocol<T> protocol, final ProtocolDataReceiver<T> receiver,
		final SmartFilter<T>[] filters, final int cacheSize, QueueOverflowStrategy strategy, final boolean autoRecover,
		final int bufferSize) {
		initBaseChannelInfo(channelKey);
		super.protocol = protocol;
		super.chain = new SmartFilterChainImpl<T>(receiver, filters);
		this.cacheSize = cacheSize;
		writeCacheQueue = new ArrayBlockingQueue<ByteBuffer>(cacheSize);
		this.strategy = strategy;
		this.autoRecover = autoRecover;
		super.bufferSize = bufferSize;
	}

	@Override
	protected void cancelReadAttention() {
		readClosed = true;
		channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_READ);
	}

	@Override
	public void close(boolean immediate) {
		super.close(immediate || writeCacheQueue.isEmpty());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.vinote.smart.socket.transport.TransportSession#close0()
	 */
	@Override
	protected void close0() {
		if (getStatus() == SessionStatusEnum.CLOSED) {
			return;
		}
		writeCacheQueue.clear();
		try {
			channelKey.channel().close();
			if (logger.isTraceEnabled()) {
				logger.trace("close connection " + channelKey.channel());
			}
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		channelKey.cancel();
		channelKey.selector().wakeup();// 必须唤醒一次选择器以便移除该Key,否则端口会处于CLOSE_WAIT状态
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

	/**
	 * 获取写缓冲
	 *
	 * @return
	 */
	public ByteBuffer getWriteBuffer() {
		if (writeBuffer != null && writeBuffer.hasRemaining()) {
			return writeBuffer;
		}

		ByteBuffer array = writeCacheQueue.poll();
		if (array != null) {
			writeBuffer = array;
			chain.doWriteFilter(this, writeBuffer);
		} else {
			writeBuffer = null;
			// 不具备写条件,移除该关注
			if (writeCacheQueue.isEmpty()) {
				synchronized (writeLock) {
					if (writeCacheQueue.isEmpty()) {
						channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_WRITE);
					}
				}
				resumeReadAttention();
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
			logger.info(getRemoteAddr() + ":" + getRemotePort() + "流控");
		}
	}

	@Override
	public void resumeReadAttention() {
		if (readClosed) {
			return;
		}
		if ((channelKey.interestOps() & SelectionKey.OP_READ) != SelectionKey.OP_READ) {
			channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_READ);
			logger.debug(getRemoteAddr() + ":" + getRemotePort() + "释放流控");
		}
	}

	@Override
	public String toString() {
		return "Session [channel=" + channelKey.channel() + ", protocol=" + protocol + ", receiver=" + ", getClass()="
			+ getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}

	@Override
	public void write(ByteBuffer buffer) throws IOException {
		buffer.flip();
		if ((writeBuffer == null || !writeBuffer.hasRemaining()) && writeCacheQueue.isEmpty()) {
			synchronized (this) {
				if ((writeBuffer == null || !writeBuffer.hasRemaining()) && writeCacheQueue.isEmpty()) {
					writeBuffer = buffer;
					chain.doWriteFilter(this, writeBuffer);
					((SocketChannel) channelKey.channel()).write(writeBuffer);
					if (writeBuffer.hasRemaining()) {
						synchronized (writeLock) {
							channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_WRITE);
							channelKey.selector().wakeup();
						}
					} else {
						writeBuffer = null;
					}
					return;
				}
			}
		}
		try {
			switch (strategy) {
			case DISCARD:
				if (!writeCacheQueue.offer(buffer)) {
					logger.warn("cache is full now");
					throw new CacheFullException("cache is full now");
				}
				break;
			case WAIT:
				writeCacheQueue.put(buffer);
				break;
			default:
				throw new QueueOverflowStrategyException("Invalid overflow strategy " + strategy);
			}

		} catch (CacheFullException e) {
			logger.warn(e.getMessage(), e);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		} finally {
			if (!channelKey.isValid()) {
				if (autoRecover) {
					throw new NotYetReconnectedException("Network anomaly, will reconnect");
				} else {
					writeCacheQueue.clear();
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
	public void write(T data) throws IOException, CacheFullException {
		throw new UnsupportedOperationException();
	}

}
