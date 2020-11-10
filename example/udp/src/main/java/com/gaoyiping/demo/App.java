package com.gaoyiping.demo;

import java.io.IOException;

import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.UdpBootstrap;

public class App {
    public static void main( String[] args ) throws IOException {
    	int socketPort = 10086;
//    	AioQuickServer<byte[]> server = new AioQuickServer<byte[]>(socketPort, new DemoProtocol(), new DemoService());
		UdpBootstrap<byte[]> server = new UdpBootstrap<>(new DemoProtocol(), new DemoService());
		server.setReadBufferSize(1024);
		server.open(socketPort);

//		server.setBannerEnabled(false); // 关掉万恶的宣传广告 (笑~~)
		//server.setThreadNum(100);
//		server.start();
    }
}
