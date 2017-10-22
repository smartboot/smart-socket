package org.smartboot.socket.protocol.p2p.message;

/**
 * 
 * <p>
 * 消息头,消息头长度固定32字节;
 * <p>
 * 注：消息体对象不要重复用于绑定消息实体
 * <p>
 * MAGIC_NUMBER:幻数(4字节)
 * <p>
 * Length：消息总长度(4个字节)
 * <p>
 * MessageType：消息类型(4个字节)
 * <p>
 * SequenceID：由发送方填写，请求和响应消息必须保持一致(4个字节)
 * <p>
 * 预留：16字节
 * 
 * @author 三刀
 * @version HeadMessage.java, v 0.1 2015年8月24日 上午10:40:49 Seer Exp.
 */
public final class HeadMessage {
	public static final int HEAD_MESSAGE_LENGTH = 32;
	/**
	 * P2P消息幻数
	 */
	public static final int MAGIC_NUMBER = 0xfadeface;

	/**
	 * 消息长度
	 */
	private int length;

	/**
	 * 消息类型
	 */
	private int messageType;

	/**
	 * 序列号,设置为int类型,跑一辈子都基本够用
	 */
	private int sequenceID;

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getMessageType() {
		return messageType;
	}

	void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	public int getSequenceID() {
		return sequenceID;
	}

	void setSequenceID(int sequenceID) {
		this.sequenceID = sequenceID;
	}
}
