package net.vinote.smart.socket.transport.ssl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

class SSLServer {

	// 服务器端授权的用户名和密码
	private static final String USER_NAME = "principal";
	private static final String PASSWORD = "credential";
	// 服务器端保密内容
	private static final String SECRET_CONTENT = "This is confidential content from server X, for your eye!";

	private SSLServerSocket serverSocket = null;

	public SSLServer() throws Exception {
		// 通过套接字工厂，获取一个服务器端套接字
		SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory
				.getDefault();
		serverSocket = (SSLServerSocket) socketFactory.createServerSocket(7070);
	}

	private void runServer() {
		while (true) {
			try {
				System.out.println("Waiting for connection...");
				// 服务器端套接字进入阻塞状态，等待来自客户端的连接请求
				SSLSocket socket = (SSLSocket) serverSocket.accept();

				// 获取服务器端套接字输入流
				BufferedReader input = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				// 从输入流中读取客户端用户名和密码
				String userName = input.readLine();
				String password = input.readLine();

				// 获取服务器端套接字输出流
				PrintWriter output = new PrintWriter(new OutputStreamWriter(
						socket.getOutputStream()));

				// 对请求进行认证，如果通过则将保密内容发送给客户端
				if (userName.equals(USER_NAME) && password.equals(PASSWORD)) {
					output.println("Welcome, " + userName);
					output.println(SECRET_CONTENT);
				} else {
					output.println("Authentication failed, you have no  access to server X...");
				}

				// 关闭流资源和套接字资源
				output.close();
				input.close();
				socket.close();

			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	public static void main(String args[]) throws Exception {
		SSLServer server = new SSLServer();
		server.runServer();
	}
}