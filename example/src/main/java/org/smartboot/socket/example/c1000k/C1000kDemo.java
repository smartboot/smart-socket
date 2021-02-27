/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: C1000kDemo.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.c1000k;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * ulimit -HSn 4000000
 * ulimit -c unlimited
 * cat /proc/<pid>/limits openstack soft nofile 4000000 root soft nofile 4000000 root hard nofile 4000000
 * @author 三刀
 * @version V1.0 , 2019/2/24
 */
public class C1000kDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(C1000kDemo.class);

    public static void main(String[] args) throws Exception {
        if(args==null||args.length==0){
            args=new String[]{"localhost"};
        }
        System.setProperty("smart-socket.client.page.isDirect", "false");
        System.setProperty("smart-socket.server.page.isDirect", "true");
        System.setProperty("smart-socket.server.pageSize", "" + (1024 * 1024 * 16));
        System.setProperty("smart-socket.client.pageSize", "1");
        MessageProcessor processor = new MessageProcessor() {
            @Override
            public void process(AioSession session, Object msg) {
            }

            @Override
            public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if(throwable!=null){
                    throwable.printStackTrace();
                }
            }
        };

        AbstractMessageProcessor processor1 = new AbstractMessageProcessor() {

            @Override
            public void process0(AioSession session, Object msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if(throwable!=null){
                    throwable.printStackTrace();
                }
            }
        };
        processor1.addPlugin(new MonitorPlugin(5));

        int serverPort = 8888;

        //启动服务端
        new AioQuickServer(serverPort, null, processor1)
                .setReadBufferSize(1).start();

        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        if (args != null) {
            for (String ip : args) {
                Executors.newFixedThreadPool(10).execute(new Runnable() {
                    int i = 4000;

                    @Override
                    public void run() {
//                        int i = 10000;
                        while (i-- > 0) {
                            try {
                                new AioQuickClient(ip, serverPort, null, processor)
                                        .setReadBufferSize(1)
                                        .start(channelGroup);
                            } catch (Exception e) {
                                LOGGER.error("exception", e);
                                break;
                            }
                        }
                    }
                });


            }
        }

    }
}
