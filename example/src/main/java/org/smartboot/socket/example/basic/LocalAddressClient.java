/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: LocalAddressClient.java
 * Date: 2021-02-28
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.basic;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2019/2/22
 */
public class LocalAddressClient {
    public static void main(String[] args) throws Exception {
        MessageProcessor processor = new MessageProcessor() {
            @Override
            public void process(AioSession session, Object msg) {

            }

            @Override
            public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        };

        String serverIp = "127.0.0.1";
        int serverPort = 8888;
        String localIp1 = "127.0.0.1";
        String localIp2 = "192.168.0.107";

        //启动服务端
        new AioQuickServer(serverPort, null, processor).start();

        //IP、端口系统指定
        new AioQuickClient(serverIp, serverPort, null, processor)
                .start();
        //指定IP、端口
        new AioQuickClient(serverIp, serverPort, null, processor)
                .bindLocal(localIp2, 8080).start();
        //指定IP、端口
        new AioQuickClient(serverIp, serverPort, null, processor)
                .bindLocal(localIp1, 8080).start();
        //指定IP、端口随机
        new AioQuickClient(serverIp, serverPort, null, processor)
                .bindLocal(localIp2, 0).start();
        //指定IP、端口随机
        new AioQuickClient(serverIp, serverPort, null, processor)
                .bindLocal(localIp1, 0).start();
        //指定端口
        new AioQuickClient(serverIp, serverPort, null, processor)
                .bindLocal(null, 8081).start();
    }
}
