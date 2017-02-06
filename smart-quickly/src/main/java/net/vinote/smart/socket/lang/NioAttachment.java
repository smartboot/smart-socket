package net.vinote.smart.socket.lang;

import net.vinote.smart.socket.transport.nio.NioSession;

/**
 * NIO attachment
 * 
 * @author seer
 * @version Attachment.java, v 0.1 2017年2月6日 下午1:59:16 Seer Exp.
 */
public class NioAttachment {
	/** 当前管道的Session对象 */
	private NioSession<?> session;

	private int curSelectionOP;

	/**
	 * Getter method for property <tt>session</tt>.
	 *
	 * @return property value of session
	 */
	public final NioSession<?> getSession() {
		return session;
	}

	/**
	 * Setter method for property <tt>session</tt>.
	 *
	 * @param session
	 *            value to be assigned to property session
	 */
	public final void setSession(NioSession<?> session) {
		this.session = session;
	}

	/**
	 * Getter method for property <tt>curSelectionOP</tt>.
	 *
	 * @return property value of curSelectionOP
	 */
	public final int getCurSelectionOP() {
		return curSelectionOP;
	}

	/**
	 * Setter method for property <tt>curSelectionOP</tt>.
	 *
	 * @param curSelectionOP
	 *            value to be assigned to property curSelectionOP
	 */
	public final void setCurSelectionOP(int curSelectionOP) {
		this.curSelectionOP = curSelectionOP;
	}

}
