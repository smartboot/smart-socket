package net.vinote.smart.socket.service.filter.impl;

import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.SmartFilterChain;
import net.vinote.smart.socket.service.process.ProtocolDataReceiver;
import net.vinote.smart.socket.transport.IoSession;

import java.nio.ByteBuffer;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 * @version SmartFilterChainImpl.java, v 0.1 2015年8月26日 下午5:08:31 Seer Exp.
 */
public class SmartFilterChainImpl<T> implements SmartFilterChain<T> {
	private ProtocolDataReceiver<T> receiver;
	private SmartFilter<T>[] handlers = null;
	private boolean hasHandlers=false;
	public SmartFilterChainImpl(ProtocolDataReceiver<T> receiver, SmartFilter<T>[] handlers) {
		this.receiver = receiver;
		this.handlers = handlers;
		this.hasHandlers=(handlers != null && handlers.length > 0);
	}

	public void doReadFilter(IoSession<T> session, T dataEntry) {
		if (dataEntry == null) {
			return;
		}
		// 接收到的消息进行预处理
		if (hasHandlers) {
			for (SmartFilter<T> h : handlers) {
				h.readFilter(session, dataEntry);
			}
		}
		boolean succ = receiver.receive(session, dataEntry);
		// 未能成功接受消息
		if (!succ && hasHandlers) {
			for (SmartFilter<T> h : handlers) {
				h.receiveFailHandler(session, dataEntry);
			}
		}

	}

//	@Override
//	public void doWriteFilterStart(IoSession<T> session, ByteBuffer buffer) {
//		if (hasHandlers) {
//			for (SmartFilter<T> h : handlers) {
//				h.beginWriteFilter(session, buffer);
//			}
//		}
//	}
//
//	@Override
//	public void doWriteFilterContinue(IoSession<T> session, ByteBuffer buffer) {
//		if (hasHandlers) {
//			for (SmartFilter<T> h : handlers) {
//				h.continueWriteFilter(session, buffer);
//			}
//		}
//	}
//
//	@Override
//	public void doWriteFilterFinish(IoSession<T> session, ByteBuffer buffer) {
//		if (hasHandlers) {
//			for (SmartFilter<T> h : handlers) {
//				h.finishWriteFilter(session, buffer);
//			}
//		}
//	}

	@Override
	public void doProcessFilter(IoSession<T> session, T d) {
		if(hasHandlers){
			for (SmartFilter<T> h : handlers) {
				h.processFilter(session, d);
			}
		}
	}

}
