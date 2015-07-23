package net.vinote.smart.socket.transport.ssl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class SSLTest {
	// 定义了监听端口号
	private final static int LISTEN_PORT = 54321;

	public static void main(String args[]) throws IOException {
		SSLServerSocket serverSocket = null;
		SSLSocket clientSocket = null;
		// 使用默认方式获取套接字工厂实例
		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory
				.getDefault();

		try {
			serverSocket = (SSLServerSocket) ssf
					.createServerSocket(LISTEN_PORT);
			// 设置不需要验证客户端身份
			serverSocket.setNeedClientAuth(false);
			System.out.println("SSLServer is listening on " + LISTEN_PORT
					+ " port");
			// 循环监听端口，如果有客户端连入就新开一个线程与之通信
			while (true) {
				// 接受新的客户端连接
				clientSocket = (SSLSocket) serverSocket.accept();
				ClientConnection clientConnection = new ClientConnection(
						clientSocket);
				// 启动一个新的线程
				Thread clientThread = new Thread(clientConnection);
				System.out.println("Client " + clientThread.getId()
						+ " is connected");
				clientThread.run();
			}
		} catch (IOException ioExp) {
			ioExp.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			serverSocket.close();
		}
	}
}

class ClientConnection implements Runnable {
	private Socket clientSocket = null;

	public ClientConnection(SSLSocket sslsocket) {
		clientSocket = sslsocket;
	}

	public void run() {
		BufferedReader reader = null;
		// 将接收到的来自客户端的文字打印出来
		try {
			reader = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					System.out.println("Communication end.");
					break;
				}
				System.out.println("Receive message: " + line);
			}
			reader.close();
			clientSocket.close();
		} catch (IOException ioExp) {
			ioExp.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
