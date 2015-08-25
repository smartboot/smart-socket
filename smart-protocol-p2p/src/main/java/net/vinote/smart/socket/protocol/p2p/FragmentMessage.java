package net.vinote.smart.socket.protocol.p2p;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import net.vinote.smart.socket.logger.RunLogger;

/**
 * 
 * 消息片段
 * 
 * @author Administrator
 * 
 */
public class FragmentMessage extends BaseMessage {
	private int length;

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
		BaseMessage baseMsg = null;
		// 至少需要确保读取到的数据字节数与解析消息头获得的消息体大小一致
		if (head.getLength() == getData().length) {
			Class<?> c = BaseMessageFactory.getInstance().getBaseMessage(
					head.getMessageType());
			if (c != null) {
				try {
					baseMsg = (BaseMessage) c.newInstance();
				} catch (Exception e) {
					RunLogger.getLogger().log(e);
				}
			} else {
				RunLogger.getLogger().log(
						Level.WARNING,
						"Message[0x"
								+ Integer.toHexString(head.getMessageType())
								+ "] Could not find class");
			}
		}
		if (baseMsg != null) {
			baseMsg.setData(getData());
			baseMsg.decode();
		}
		return baseMsg;
	}
}
