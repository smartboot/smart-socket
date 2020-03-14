/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpDemo.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.benchmark;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.UdpBootstrap;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/16
 */
public class UdpServer {
    public static void main(String[] args) throws IOException, InterruptedException {

        //服务端
        final UdpBootstrap<String> bootstrap = new UdpBootstrap<String>(new StringProtocol(), new MessageProcessor<String>() {
            @Override
            public void process(AioSession<String> channel, String msg) {
                try {
                    InetSocketAddress remoteAddress = channel.getRemoteAddress();
                    if (remoteAddress.getPort() == 9999) {
                        System.out.println(channel + " receive response:" + msg);
                    } else {
                        System.out.println("server receive request:" + msg);
                        WriteBuffer buffer = channel.writeBuffer();
                        byte[] bytes = msg.getBytes();
                        buffer.writeInt(bytes.length);
                        buffer.write(bytes);
//                        buffer.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void stateEvent(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        });
        int threadNum = Runtime.getRuntime().availableProcessors();
        bootstrap.setThreadNum(threadNum);
        bootstrap.setReadBufferSize(1024);
        bootstrap.open(9999);
        System.out.println("启动成功");
    }
}
