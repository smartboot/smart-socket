package net.vinote.smart.socket.protocol.p2p;

import java.net.ProtocolException;
import java.util.concurrent.atomic.AtomicInteger;

import net.vinote.smart.socket.exception.DecodeException;
import net.vinote.smart.socket.protocol.DataEntry;

/**
 * P2P协议基础消息体对象
 *
 * @author Seer
 * @version BaseMessage.java, v 0.1 2015年8月22日 上午11:24:03 Seer Exp.
 */
public abstract class BaseMessage extends DataEntry {
	/** 消息头 */
	private HeadMessage head;

	private static AtomicInteger sequence = new AtomicInteger(0);

	public BaseMessage(HeadMessage head) {
		this.head = head;
	}

	public BaseMessage() {
		head = new HeadMessage();
	}

	public final HeadMessage getHead() {
		return head;
	}

	/**
	 * <p>
	 * 消息编码;
	 * </p>
	 * <p>
	 * 若是请求消息,将自动为其生成唯一标识 sequenceID;<br/>
	 * 响应消息需要自行从对应的请求消息中获取再设置
	 * </p>
	 *
	 * @throws ProtocolException
	 */
	@Override
	public final byte[] encode() throws ProtocolException {
		reset(MODE.WRITE);
		// 完成消息体编码便可获取实际消息大小
		position(HeadMessage.HEAD_MESSAGE_LENGTH);// 定位至消息头末尾
		encodeBody();// 编码消息体
		if (head == null) {
			throw new ProtocolException("Protocol head is unset!");
		}
		head.setLength(getData().length);// 设置消息体长度

		// 若是请求消息,将自动为其生成唯一标识 sequenceID;重复encode不产生新的序列号
		if (head.getSequenceID() == 0
			&& (MessageType.RESPONSE_MESSAGE & getMessageType()) == MessageType.REQUEST_MESSAGE) {
			head.setSequenceID(sequence.incrementAndGet());// 由于初始值为0,所以必须先累加一次,否则获取到的是无效序列号
		}

		position(0);// 定位至消息体起始位置
		limitIndex(HeadMessage.HEAD_MESSAGE_LENGTH);// 操作位锁定,避免自定消息体编码超过消息体大小
		encodeHead();// 编码消息头
		clearLimit();
		position(head.getLength());// 设置标志位至消息末尾
		return getData();
	}

	/**
	 * 消息解码
	 *
	 * @throws ProtocolException
	 */
	@Override
	public final void decode() {
		reset(MODE.READ);
		limitIndex(HeadMessage.HEAD_MESSAGE_LENGTH);// 操作位锁定,避免自定消息体解码超过消息体大小
		decodeHead();
		clearLimit();
		position(HeadMessage.HEAD_MESSAGE_LENGTH);// 定位至消息体位置
		decodeBody();
	}

	/**
	 * 各消息类型各自实现消息体编码工作
	 *
	 * @throws ProtocolException
	 */
	protected abstract void encodeBody() throws ProtocolException;

	/**
	 * 各消息类型各自实现消息体解码工作
	 *
	 * @throws ProtocolException
	 */
	protected abstract void decodeBody() throws DecodeException;

	/**
	 * 获取消息类型
	 * 
	 * @return
	 */
	public abstract int getMessageType();

	/**
	 * 对消息头进行编码
	 */
	protected final void encodeHead() {
		// 输出幻数
		writeInt(HeadMessage.MAGIC_NUMBER);
		// 输出消息长度
		writeInt(head.getLength());
		// 消息类型
		writeInt(getMessageType());
		// 由发送方填写，请求和响应消息必须保持一致(4个字节)
		writeInt(head.getSequenceID());

	}

	/**
	 * 对消息头进行解码
	 */
	protected final void decodeHead() {
		reset(MODE.READ);
		// 读取幻数
		int magicNum = readInt();
		if (magicNum != HeadMessage.MAGIC_NUMBER) {
			throw new DecodeException("Invalid Magic Number: 0x" + Integer.toHexString(magicNum));
		}
		// 读取消息长度
		int length = readInt();
		// 消息类型
		int msgType = readInt();
		// 由发送方填写，请求和响应消息必须保持一致(4个字节)
		int sequeue = readInt();
		if (head == null) {
			head = new HeadMessage();
		}
		head.setLength(length);
		head.setMessageType(msgType);
		head.setSequenceID(sequeue);
	}

}
