package net.vinote.smart.socket.demo.http.connector;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import net.vinote.smart.socket.transport.TransportSession;

public class SmartHttpServletResponse implements HttpServletResponse {
	private PrintWriter printWriter;

	private ServletOutputStream servletOut;

	private volatile boolean writeHeader = false;

	private Map<String, String> headerMap = new HashMap<String, String>();

	private int status = 200;

	private String statusMsg = "OK";

	public SmartHttpServletResponse(TransportSession session) {
		Writer writer = new SmartWriter(session) {

			public void write(char[] cbuf, int off, int len) throws IOException {
				if (!writeHeader) {
					StringBuffer sb = new StringBuffer("HTTP/1.1 " + status
							+ " " + statusMsg + "\r\n");
					for (Entry<String, String> entry : headerMap.entrySet()) {
						sb.append(entry.getKey() + ": " + entry.getValue()
								+ "\r\n");
					}
					sb.append("\r\n");
					writeHeader = true;
					char[] buf = sb.toString().toCharArray();
					super.write(buf, 0, buf.length);
				}
				super.write(cbuf, off, len);
			}

		};
		printWriter = new SmartPrintWriter(writer);
		servletOut = new SmartServletOutputStream(writer);
	}

	public String getCharacterEncoding() {

		return null;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		return servletOut;
	}

	public PrintWriter getWriter() throws IOException {
		return printWriter;
	}

	public void setCharacterEncoding(String charset) {

	}

	public void setContentLength(int len) {

	}

	public void setContentType(String type) {

	}

	public void setBufferSize(int size) {

	}

	public int getBufferSize() {

		return 0;
	}

	public void flushBuffer() throws IOException {

	}

	public void resetBuffer() {

	}

	public boolean isCommitted() {

		return false;
	}

	public void reset() {

	}

	public void setLocale(Locale loc) {

	}

	public Locale getLocale() {

		return null;
	}

	public void addCookie(Cookie cookie) {

	}

	public boolean containsHeader(String name) {

		return false;
	}

	public String encodeURL(String url) {

		return null;
	}

	public String encodeRedirectURL(String url) {

		return null;
	}

	public String encodeUrl(String url) {

		return null;
	}

	public String encodeRedirectUrl(String url) {

		return null;
	}

	public void sendError(int sc, String msg) throws IOException {
		setStatus(sc, msg);
	}

	public void sendError(int sc) throws IOException {
		setStatus(sc);
	}

	public void sendRedirect(String location) throws IOException {

	}

	public void setDateHeader(String name, long date) {

	}

	public void addDateHeader(String name, long date) {

	}

	public void setHeader(String name, String value) {

	}

	public void addHeader(String name, String value) {
		headerMap.put(name, value);
	}

	public void setIntHeader(String name, int value) {

	}

	public void addIntHeader(String name, int value) {

	}

	public void setStatus(int sc) {
		this.status = sc;
	}

	public void setStatus(int sc, String sm) {
		this.status = sc;
		this.statusMsg = sm;
	}

	public String getContentType() {
		// TODO Auto-generated method stub
		return headerMap.get("Content-Type");
	}

}
