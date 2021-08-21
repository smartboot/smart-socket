/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StreamMonitorDemo.java
 * Date: 2021-08-21
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.plugins;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/8/21
 */
public class StreamMonitorDemo {
    public static void main(String[] args) throws IOException {
        //服务端
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                System.out.println("收到客户端请求消息: " + msg);
                byte[] content = "Hi Client".getBytes();
                try {
                    session.writeBuffer().writeInt(content.length);
                    session.writeBuffer().write(content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
            }
        };
        //注册插件
        processor.addPlugin(new StreamMonitorPlugin<>());
//        processor.addPlugin(new StreamMonitorPlugin<>(new BiConsumer<AsynchronousSocketChannel, byte[]>() {
//            @Override
//            public void accept(AsynchronousSocketChannel asynchronousSocketChannel, byte[] bytes) {
//                System.err.println("输入内容: " + new String(bytes) + " END");
//            }
//        }, new BiConsumer<AsynchronousSocketChannel, byte[]>() {
//            @Override
//            public void accept(AsynchronousSocketChannel asynchronousSocketChannel, byte[] bytes) {
//                System.err.println("输出内容: " + new String(bytes) + " END");
//            }
//        }));
        AioQuickServer server = new AioQuickServer(8080, new StringProtocol(), processor);
        server.start();

        //客户端
        AioQuickClient client = new AioQuickClient("localhost", 8080, new StringProtocol(), new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                System.out.println("收到服务端响应消息: " + msg);
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        });
        AioSession session = client.start();
        byte[] content = "HelloWorld".getBytes();
        session.writeBuffer().writeInt(content.length);
        session.writeBuffer().write(content);
        session.writeBuffer().flush();
    }
}
