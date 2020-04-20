/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslDemo.java
 * Date: 2020-04-16
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket;

import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/16
 */
public class SslDemo {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        IntegerServerProcessor serverProcessor = new IntegerServerProcessor();
        AioQuickServer sslQuickServer = new AioQuickServer(8080, new IntegerProtocol(), serverProcessor);
        SslPlugin sslServerPlugin = new SslPlugin(null);
        sslServerPlugin.initForServer(SslDemo.class.getClassLoader().getResourceAsStream("server.keystore"), "123456", "123456", ClientAuth.OPTIONAL);
        serverProcessor.addPlugin(sslServerPlugin);
        sslQuickServer.start();

        IntegerClientProcessor clientProcessor = new IntegerClientProcessor();
        AioQuickClient sslQuickClient = new AioQuickClient("localhost", 8080, new IntegerProtocol(), clientProcessor);
        SslPlugin sslPlugin = new SslPlugin(null);
        sslPlugin.initForClient(SslDemo.class.getClassLoader().getResourceAsStream("server.keystore"), "123456");
        clientProcessor.addPlugin(sslPlugin);
//        clientProcessor.addPlugin(new SslPlugin());
        AioSession aioSession = sslQuickClient.start();
        aioSession.writeBuffer().writeInt(1);
        aioSession.writeBuffer().flush();

    }
}
