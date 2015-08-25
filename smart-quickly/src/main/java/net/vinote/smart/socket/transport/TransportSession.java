package net.vinote.smart.socket.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import net.vinote.smart.socket.exception.CacheFullException;
import net.vinote.smart.socket.exception.DecodeException;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.Protocol;
import net.vinote.smart.socket.service.filter.SmartFilterChain;
import net.vinote.smart.socket.transport.enums.SessionStatusEnum;

/**
 * 传输层会话<br/>
 * 维护客户端-》服务端 或 服务端-》客户端 的当前会话
 *
 * @author Seer
 * @version TransportSession.java, v 0.1 2015年8月24日 上午10:31:38 Seer Exp.
 */
public abstract class TransportSession {

	/** 会话ID */
	private final String sessionId = String.valueOf(System
			.identityHashCode(this));
	/** 配置信息 */
	protected QuicklyConfig quickConfig;
	/** 消息通信协议 */
	protected Protocol protocol;
	// /** 消息接受器 */
	// protected ProtocolDataReceiver receiver;

	/** 缓存传输层读取到的数据流 */
	private ByteBuffer readBuffer;

	/** 会话状态 */
	private volatile SessionStatusEnum status = SessionStatusEnum.ENABLED;

	protected SmartFilterChain chain;

	/**
	 * 取消读关注<br/>
	 * 当协议解码失败时应该关闭读关注,防止异常码流继续进入服务器
	 */
	protected abstract void cancelReadAttention();

	public final void close() {
		close(true);
	}

	/**
	 * * 是否立即关闭会话
	 *
	 * @param immediate
	 *            true:立即关闭,false:响应消息发送完后关闭
	 */
	public void close(boolean immediate) {
		if (immediate) {
			synchronized (TransportSession.this) {
				close0();
				status = SessionStatusEnum.Closed;
			}
		} else {
			status = SessionStatusEnum.Closing;
		}
	}

	/**
	 * * 关闭会话 *
	 * <p>
	 * * 会话的关闭将触发Socket通道的关闭 *
	 * </p>
	 */
	protected abstract void close0();

	/**
	 * 刷新缓存的数据流,对已读取到的数据进行一次协议解码操作
	 */
	public void flushReadBuffer() {
		ByteBuffer buffer = getReadBuffer();
		buffer.flip();

		// 将从管道流中读取到的字节数据添加至当前会话中以便进行消息解析
		try {
			chain.doReadFilter(this, protocol.decode(buffer));
		} catch (DecodeException e) {
			RunLogger.getLogger().log(Level.WARNING, e.getMessage());
			cancelReadAttention();
		} finally {
			buffer.compact();
		}
	}

	public abstract String getLocalAddress();

	public final QuicklyConfig getQuickConfig() {
		return quickConfig;
	}

	public ByteBuffer getReadBuffer() {
		if (readBuffer == null) {
			readBuffer = ByteBuffer.allocate(quickConfig.getDataBufferSize());
		}
		return readBuffer;
	}

	/**
	 * Returns the Internet Protocol (IP) address of the client or last proxy
	 * that sent the request
	 *
	 * @return
	 */
	public abstract String getRemoteAddr();

	/**
	 * Returns the fully qualified name of the client or the last proxy that
	 * sent the request. If the engine cannot or chooses not to resolve the
	 * hostname (to improve performance), this method returns the dotted-string
	 * form of the IP address
	 *
	 * @return
	 */
	public abstract String getRemoteHost();

	public abstract int getRemotePort();

	/**
	 * 获取当前Session的唯一标识
	 *
	 * @return
	 */
	public final String getSessionID() {
		return sessionId;
	}

	public SessionStatusEnum getStatus() {
		return status;
	}

	/**
	 * 获取超时时间
	 *
	 * @return
	 */
	public int getTimeout() {
		return quickConfig.getTimeout();
	}

	/**
	 * 当前会话是否已失效
	 */
	public abstract boolean isValid();

	/**
	 * 暂停读关注
	 */
	public abstract void pauseReadAttention();

	/**
	 * 恢复读关注
	 */
	public abstract void resumeReadAttention();

	/**
	 * * 将参数中传入的数据输出至对端;处于性能考虑,通常对数据进行缓存处理
	 *
	 * @param data
	 *            输出数据至对端
	 * @return 是否输出成功
	 * @throws Exception
	 */
	public abstract void write(byte[] data) throws IOException,
			CacheFullException;

	/**
	 * * 将参数中传入的数据输出至对端;处于性能考虑,通常对数据进行缓存处理
	 *
	 * @param data
	 *            输出数据至对
	 * @return 是否输出成功
	 * @throws Exception
	 */
	public abstract void write(DataEntry data) throws IOException,
			CacheFullException;

}
