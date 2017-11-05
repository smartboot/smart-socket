package org.smartboot.socket.protocol.http.servlet.core;

import java.io.IOException;

/**
 * Indicates an I/O exception writing to client.
 * 
 * @author Kohsuke Kawaguchi
 */
public class ClientSocketException extends IOException {
	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -8077788444680218850L;

	/**
	 * Build a new instance of ClientSocketException.
	 * 
	 * @param cause
	 */
	public ClientSocketException(Throwable cause) {
		super("Failed to write to client");
		initCause(cause);
	}
}
