package net.vinote.smart.socket.protocol.p2p;

import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;

/**
 * 消息处理Handler
 * 
 * @author seer
 * @version MessageHandler.java, v 0.1 2017年1月18日 上午11:18:28 Seer Exp.
 */
public abstract class MessageHandler {
	public void init() {
	}

	public abstract void handler(Session<BaseMessage> session, BaseMessage message);

	public void destory() {
	}
}
