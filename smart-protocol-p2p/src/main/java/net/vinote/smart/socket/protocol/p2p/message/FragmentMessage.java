package net.vinote.smart.socket.protocol.p2p.message;

import java.util.Map;
import java.util.WeakHashMap;

import net.vinote.smart.socket.service.factory.ServiceMessageFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 消息片段
 *
 * @author Administrator
 *
 */
public class FragmentMessage extends BaseMessage {
	private Logger logger = LoggerFactory.getLogger(FragmentMessage.class);
	private Map<Class<?>, CacheUnit> cacheMap = new WeakHashMap<Class<?>, CacheUnit>();

	@Override
	protected void encodeBody() {
		throw new RuntimeException("unsupport method");
	}

	@Override
	protected void decodeBody() {
		throw new RuntimeException("unsupport method");
	}

	@Override
	public int getMessageType() {
		return 0;
	}

	/**
	 * 将当前对象中的数据解析成具体类型的消息体
	 *
	 * @return
	 */
	public BaseMessage decodeMessage(ServiceMessageFactory messageFactory) {
		position(0);
		decodeHead();
		HeadMessage head = getHead();

		// 至少需要确保读取到的数据字节数与解析消息头获得的消息体大小一致
		if (head.getLength() != getData().limit()) {
			return null;
		}
		Class<?> c = null;
		if (messageFactory instanceof P2pServiceMessageFactory) {
			c = ((P2pServiceMessageFactory) messageFactory).getBaseMessage(head.getMessageType());
		} else {
			throw new IllegalArgumentException("invalid ServiceMessageFactory " + messageFactory);
		}
		if (c == null) {
			logger.warn("Message[0x" + Integer.toHexString(head.getMessageType()) + "] Could not find class");
			return null;
		}

		CacheUnit cache = cacheMap.get(c);
		BaseMessage baseMsg = null;
		boolean hasHead = false;
		if (cache == null) {
			try {
				// 优先调用带HeadMessage参数的构造方法,减少BaseMessage中构造HeadMessage对象的次数
				baseMsg = (BaseMessage) c.getConstructor(HeadMessage.class).newInstance(head);
				hasHead = true;
			} catch (NoSuchMethodException e) {
				try {
					baseMsg = (BaseMessage) c.newInstance();
				} catch (Exception e1) {
					logger.warn("", e1);
				}
			} catch (Exception e) {
				logger.warn("", e);
			}
			if (baseMsg == null) {
				return null;
			}
			cache = new CacheUnit();
			cache.hasHead = hasHead;
			cache.message = baseMsg;
		} else {
			try {
				baseMsg = (BaseMessage) cache.message.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
			hasHead = cache.hasHead;
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

	class CacheUnit {
		BaseMessage message;
		boolean hasHead;
	}
}
