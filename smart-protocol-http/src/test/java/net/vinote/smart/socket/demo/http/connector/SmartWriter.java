package net.vinote.smart.socket.demo.http.connector;

import java.io.IOException;
import java.io.Writer;

import net.vinote.smart.socket.exception.CacheFullException;
import net.vinote.smart.socket.transport.TransportSession;

public class SmartWriter extends Writer {
	private TransportSession tsession;

	public SmartWriter(TransportSession tsession) {
		this.tsession = tsession;
	}

	
	public void write(final char[] cbuf, final int off, final int len)
			throws IOException {
		try {
			tsession.write(new String(cbuf, off, len).getBytes());
		} catch (CacheFullException e) {
			throw new IOException(e);
		}
	}

	
	public void flush() throws IOException {
		// tsession.flushReadBuffer();
	}

	
	public void close() throws IOException {
		tsession.close(false);
	}

}
