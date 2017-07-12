package net.vinote.smart.socket.transport.nio;

/**
 * NIO attachment
 * 
 * @author seer
 * @version Attachment.java, v 0.1 2017年2月6日 下午1:59:16 Seer Exp.
 */
 class NioAttachment {
	/** 当前管道的Session对象 */
	private NioSession<?> session;

	public NioAttachment(NioSession<?> session) {
		this.session = session;
	}

	/**
	 * Getter method for property <tt>session</tt>.
	 *
	 * @return property value of session
	 */
	public final NioSession<?> getSession() {
		return session;
	}

}
