package net.vinote.smart.socket.exception;

/**
 * 消息编码码异常
 * 
 * @author Seer
 * 
 */
public class EncodeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EncodeException(String string) {
		super(string);
	}

	public EncodeException(Throwable cause) {
		super(cause);
	}

}
