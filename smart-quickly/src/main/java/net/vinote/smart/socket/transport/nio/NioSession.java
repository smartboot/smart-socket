package net.vinote.smart.socket.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.exception.NotYetReconnectedException;
import net.vinote.smart.socket.exception.QueueOverflowStrategyException;
import net.vinote.smart.socket.lang.ArrayBlockingQueue;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.service.filter.impl.SmartFilterChainImpl;
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
	private BlockingQueue<ByteBuffer> writeCacheQueue;

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
	public NioSession(SelectionKey channelKey, final QuicklyConfig<T> config) {
		super(ByteBuffer.allocate(config.getDataBufferSize()));
		initBaseChannelInfo(channelKey);
		super.protocol = config.getProtocolFactory().createProtocol();
		super.chain = new SmartFilterChainImpl<T>(config.getProcessor(), config.getFilters());
		this.cacheSize = config.getCacheSize();
		writeCacheQueue = new ArrayBlockingQueue(cacheSize);
		this.strategy = QueueOverflowStrategy.valueOf(config.getQueueOverflowStrategy());
		this.autoRecover = config.isAutoRecover();
		super.bufferSize = config.getDataBufferSize();
		super.timeout = config.getTimeout();
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
	public final ByteBuffer getWriteBuffer() {
		ByteBuffer buffer = null;
		// 移除已输出的数据流
		while ((buffer = writeCacheQueue.peek()) != null && buffer.remaining() == 0) {
			writeCacheQueue.remove(buffer);// 不要用poll,因为该行线程不安全
		}

		// 缓存队列已空则注销写关注
		if (buffer == null) {
			synchronized (writeLock) {
				if (writeCacheQueue.isEmpty()) {
					channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_WRITE);
				}
			}
			resumeReadAttention();
			return null;
		} /*
			 * else if (buffer.position() == 0) {// 首次输出执行过滤器
			 * chain.doWriteFilter(this, buffer); }
			 */
		return buffer;
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
	public final void pauseReadAttention() {
		if ((channelKey.interestOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
			channelKey.interestOps(channelKey.interestOps() & ~SelectionKey.OP_READ);
			logger.info(getRemoteAddr() + ":" + getRemotePort() + "流控");
		}
	}

	@Override
	public final void resumeReadAttention() {
		if (readClosed) {
			return;
		}
		if ((channelKey.interestOps() & SelectionKey.OP_READ) != SelectionKey.OP_READ) {
			channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_READ);
			if (logger.isDebugEnabled()) {
				logger.debug(getRemoteAddr() + ":" + getRemotePort() + "释放流控");
			}
		}
	}

	@Override
	public String toString() {
		return "Session [channel=" + channelKey.channel() + ", protocol=" + protocol + ", receiver=" + ", getClass()="
			+ getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}

	@Override
	public final void write(ByteBuffer buffer) throws IOException {
		chain.doWriteFilter(this, buffer);
		boolean isNew = true;

		buffer.flip();
		// 队列为空时直接输出
		if (writeCacheQueue.isEmpty()) {
			synchronized (this) {
				if (writeCacheQueue.isEmpty()) {
					// chain.doWriteFilter(this, buffer);
					int writeTimes = 8;// 控制循环次数防止低效输出流占用资源
					while (((SocketChannel) channelKey.channel()).write(buffer) > 0 && writeTimes >> 1 > 0)
						;
					// 数据全部输出则return
					if (buffer.position() >= buffer.limit()) {
						return;
					}

					boolean cacheFlag = writeCacheQueue.offer(buffer);
					// 已输出部分数据，但剩余数据缓存失败,则异常处理
					if (!cacheFlag && buffer.position() > 0) {
						throw new IOException("cache data fail, channel has become unavailable!");
					}
					// 缓存失败并无数据输出,则忽略本次数据包
					if (!cacheFlag && buffer.position() == 0) {
						logger.warn("cache data fail, ignore!");
						return;
					}
					isNew = false;
				}
			}
		}
		// 若当前正阻塞于读操作，则尽最大可能进行写操作
//		else {
//			NioAttachment attach = (NioAttachment) channelKey.attachment();
//			ByteBuffer preBuffer = null;
//			while (attach.getCurSelectionOP() == SelectionKey.OP_READ && (preBuffer = getWriteBuffer()) != null
//				&& ((SocketChannel) channelKey.channel()).write(preBuffer) > 0) {
//				System.out.println("haha");
//				;
//			}
//		}

		try {
			if (isNew) {
				switch (strategy) {
				case DISCARD:
					if (!writeCacheQueue.offer(buffer)) {
						logger.warn("cache is full now," + StringUtils.toHexString(buffer.array()));
					}
					break;
				case WAIT:
					writeCacheQueue.put(buffer);
					break;
				default:
					throw new QueueOverflowStrategyException("Invalid overflow strategy " + strategy);
				}
			}
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		} finally {
			if (channelKey.isValid()) {
				synchronized (writeLock) {
					channelKey.interestOps(channelKey.interestOps() | SelectionKey.OP_WRITE);
					channelKey.selector().wakeup();
				}
			} else {
				if (autoRecover) {
					throw new NotYetReconnectedException("Network anomaly, will reconnect");
				} else {
					writeCacheQueue.clear();
					throw new IOException("Channel is invalid now!");
				}
			}
		}

	}

	/*
	 * 将数据输出至缓存,若缓存已满则返回false (non-Javadoc)
	 * 
	 * @see com.zjw.platform.quickly.Session#write(byte[])
	 */
	// @Override
	// public void write(T data) throws IOException {
	// throw new UnsupportedOperationException();
	// }

}
