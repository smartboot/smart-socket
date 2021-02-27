/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: App.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.udp;

import java.io.IOException;

import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.UdpBootstrap;

public class App {
    public static void main( String[] args ) throws IOException {
    	int socketPort = 10086;
//    	AioQuickServer<byte[]> server = new AioQuickServer<byte[]>(socketPort, new DemoProtocol(), new DemoService());
		UdpBootstrap server = new UdpBootstrap(new DemoProtocol(), new DemoService());
		server.setReadBufferSize(1024);
		server.open(socketPort);

//		server.setBannerEnabled(false); // 关掉万恶的宣传广告 (笑~~)
		//server.setThreadNum(100);
//		server.start();
    }
}
