/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IPBlackListPluginDemo.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket.plugin;

import com.smartboot.socket.decoder.NullProtocol;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.BlackListPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/3/27
 */
public class IPBlackListPluginDemo {
    public static void main(String[] args) throws IOException {
        AbstractMessageProcessor processor = new AbstractMessageProcessor() {
            @Override
            public void process0(AioSession session, Object msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                switch (stateMachineEnum) {
                    case REJECT_ACCEPT:
                        System.out.println("连接被拒绝,session:" + session);
                        break;
                    case NEW_SESSION:
                        System.out.println("建立连接,session:" + session);
                        session.close();
                    default:
                        System.out.println("状态机:" + stateMachineEnum + " ,session:" + session);

                }
            }
        };
        BlackListPlugin ipBlackListPlugin = new BlackListPlugin();
        ipBlackListPlugin.addRule(new BlackListPlugin.BlackListRule() {
            @Override
            public boolean access(InetSocketAddress address) {
                String ip = address.getAddress().getHostAddress();
                return !"127.0.0.1".equals(ip);
            }
        });
        processor.addPlugin(ipBlackListPlugin);
        AioQuickServer aioQuickServer = new AioQuickServer(8080, new NullProtocol(), processor);
        aioQuickServer.start();
    }
}
