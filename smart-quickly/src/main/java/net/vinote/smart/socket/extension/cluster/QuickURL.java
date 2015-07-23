package net.vinote.smart.socket.extension.cluster;

public class QuickURL {
	private String ip;
	private int port;

	public QuickURL() {
	}

	public QuickURL(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
