package net.vinote.smart.socket.demo.http.connector;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletOutputStream;

public class SmartServletOutputStream extends ServletOutputStream {
	private Writer write;

	public SmartServletOutputStream(Writer write) {
		this.write = write;
	}

	public void write(int b) throws IOException {
		write.write(b);
	}

}
