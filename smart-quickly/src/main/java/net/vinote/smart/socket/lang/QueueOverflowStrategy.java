package net.vinote.smart.socket.lang;

/**
 * 队列溢出处理策略
 * 
 * @author Seer
 *
 */
public enum QueueOverflowStrategy {

	/** 等待 */
	WAIT,
	/** 丢弃新消息 */
	DISCARD
}
