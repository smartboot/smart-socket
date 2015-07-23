package net.vinote.smart.socket.demo.http.connector;

import java.io.PrintWriter;
import java.io.Writer;

public class SmartPrintWriter extends PrintWriter {

	public SmartPrintWriter(Writer out) {
		super(out);
	}
}
