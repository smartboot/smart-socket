package org.smartboot.socket.protocol.http.servlet;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrap an {@link OutputStream} so that we can distinguish errors writing to
 * clients.
 * 
 * <p>
 * Those are normally caused by a browser aborting a connection, and note
 * worthwhile to report to log.
 * 
 * @author Kohsuke Kawaguchi
 */
public class ClientOutputStream extends OutputStream {
	private final OutputStream out;

	/**
	 * Build a new instance of ClientOutputStream.
	 * @param out
	 */
	public ClientOutputStream(OutputStream out) {
		this.out = out;
	}

	public void write(int b) throws ClientSocketException {
		try {
			out.write(b);
		} catch (IOException e) {
			throw new ClientSocketException(e);
		}
	}

	public void write(byte[] b) throws ClientSocketException {
		try {
			out.write(b);
		} catch (IOException e) {
			throw new ClientSocketException(e);
		}
	}

	public void write(byte[] b, int off, int len) throws ClientSocketException {
		try {
			out.write(b, off, len);
		} catch (IOException e) {
			throw new ClientSocketException(e);
		}
	}

	public void flush() throws IOException {
		out.flush();
	}

	public void close() throws IOException {
		out.close();
	}
}
