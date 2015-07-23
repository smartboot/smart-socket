package net.vinote.smart.socket.exception;

/**
 * 队列溢出处理策略异常
 * 
 * @author Seer
 *
 */
public class QueueOverflowStrategyException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -194800101189387672L;

	public QueueOverflowStrategyException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public QueueOverflowStrategyException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public QueueOverflowStrategyException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public QueueOverflowStrategyException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
