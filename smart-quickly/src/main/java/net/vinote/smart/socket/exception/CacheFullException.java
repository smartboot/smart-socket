package net.vinote.smart.socket.exception;

/**
 * 缓冲区已满异常
 * 
 * @author Seer
 *
 */
public class CacheFullException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6468792898769591728L;

	public CacheFullException() {
		super();
	}

	public CacheFullException(String message, Throwable cause) {
		super(message, cause);
	}

	public CacheFullException(String message) {
		super(message);
	}

	public CacheFullException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
