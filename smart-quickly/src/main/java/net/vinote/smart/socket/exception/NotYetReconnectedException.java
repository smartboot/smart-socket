package net.vinote.smart.socket.exception;

public class NotYetReconnectedException extends IllegalStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6827370185304536703L;

	/**
	 * Constructs an instance of this class.
	 */
	public NotYetReconnectedException() {
	}

	public NotYetReconnectedException(String s) {
		super(s);
	}

}
