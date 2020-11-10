package com.gaoyiping.demo;

import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.UdpBootstrap;
import org.smartboot.socket.transport.UdpChannel;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class AppClient {
    public static void main( String[] args ) throws IOException {

		final UdpBootstrap<byte[]> server = new UdpBootstrap<byte[]>(new DemoProtocol(), new DemoClient());

    	int socketPort = 10086;
//    	AioQuickServer<byte[]> server = new AioQuickServer<byte[]>(socketPort, new DemoProtocol(), new DemoService());
//		UdpBootstrap<byte[]> server = new UdpBootstrap<>(new DemoProtocol(), new DemoService());
		server.setReadBufferSize(1024);
//		server.open(socketPort);

		int i = 1;
		final SocketAddress remote = new InetSocketAddress("localhost", socketPort);
		while (i-- > 0) {
			new Thread(() -> {
				try {
					int count = 100;
					UdpChannel<byte[]> channel = server.open();
					AioSession aioSession = channel.connect(remote);
					WriteBuffer writeBuffer = aioSession.writeBuffer();
					while (count-- > 0) {
						byte[] msg = ("HelloWorld" + count).getBytes();
						writeBuffer.writeInt(msg.length);
						writeBuffer.write(msg);
						writeBuffer.flush();
					}
					System.out.println("发送完毕");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		}

//		server.setBannerEnabled(false); // 关掉万恶的宣传广告 (笑~~)
		//server.setThreadNum(100);
//		server.start();
    }
}
