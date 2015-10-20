package net.vinote.smart.socket.protocol.p2p.message;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;

/**
 * 
 * 消息片段
 * 
 * @author Administrator
 * 
 */
public class FragmentMessage extends BaseMessage {
	private int length;
	private QuicklyConfig quicklyConfig;

	public FragmentMessage(QuicklyConfig quicklyConfig) {
		this.quicklyConfig = quicklyConfig;
	}

	protected void encodeBody() {
		throw new RuntimeException("unsupport method");
	}

	protected void decodeBody() {
		throw new RuntimeException("unsupport method");
	}

	public int getMessageType() {
		return 0;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getReadSize() {
		return getData() == null ? 0 : getData().length;
	}

	public void append(ByteBuffer buf, int size) {
		append(buf.array(), buf.position(), size);
		buf.position(buf.position() + size);
	}

	public void append(byte[] data, int index, int length) {
		byte[] tempData = null;
		if (getData() == null || getData().length == 0) {
			tempData = new byte[length];
			System.arraycopy(data, index, tempData, 0, length);
		} else {
			tempData = new byte[getData().length + length];
			System.arraycopy(getData(), 0, tempData, 0, getData().length);
			System.arraycopy(data, index, tempData, getData().length, length);
		}
		setData(tempData);
	}

	public void reset() {
		length = 0;
		setData(null);
	}

	/**
	 * 将当前对象中的数据解析成具体类型的消息体
	 * 
	 * @return
	 */
	public BaseMessage decodeMessage() {
		decodeHead();
		HeadMessage head = getHead();

		// 至少需要确保读取到的数据字节数与解析消息头获得的消息体大小一致
		if (head.getLength() != getData().length) {
			return null;
		}
		ServiceMessageFactory messageFactory = quicklyConfig.getServiceMessageFactory();
		Class<?> c = null;
		if (messageFactory instanceof P2pServiceMessageFactory) {
			c = ((P2pServiceMessageFactory) messageFactory).getBaseMessage(head.getMessageType());
		} else {
			throw new IllegalArgumentException("invalid ServiceMessageFactory " + messageFactory);
		}
		if (c == null) {
			RunLogger.getLogger().log(Level.WARNING,
					"Message[0x" + Integer.toHexString(head.getMessageType()) + "] Could not find class");
			return null;
		}

		BaseMessage baseMsg = null;
		boolean hasHead = false;
		try {
			// 优先调用带HeadMessage参数的构造方法,减少BaseMessage中构造HeadMessage对象的次数
			baseMsg = (BaseMessage) c.getConstructor(HeadMessage.class).newInstance(head);
			hasHead = true;
		} catch (NoSuchMethodException e) {
			try {
				baseMsg = (BaseMessage) c.newInstance();
			} catch (Exception e1) {
				RunLogger.getLogger().log(e1);
			}
		} catch (Exception e) {
			RunLogger.getLogger().log(e);
		}
		if (baseMsg == null) {
			return null;
		}

		baseMsg.setData(getData());
		// 加密的消息体暂不解码
		if (head.isSecure()) {
			if (!hasHead) {
				baseMsg.decodeHead();// 解码消息头以便后续解密处理
			}
		} else {
			baseMsg.decode();
		}
		return baseMsg;
	}
}
