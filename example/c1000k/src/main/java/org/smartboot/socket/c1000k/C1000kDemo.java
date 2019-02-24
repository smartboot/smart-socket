package org.smartboot.socket.c1000k;

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
import java.util.concurrent.ThreadFactory;

/**
 * @author 三刀
 * @version V1.0 , 2019/2/24
 */
public class C1000kDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(C1000kDemo.class);

    public static void main(String[] args) throws Exception {
        MessageProcessor processor = new MessageProcessor() {
            @Override
            public void process(AioSession session, Object msg) {
            }

            @Override
            public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
            }
        };

        AbstractMessageProcessor processor1 = new AbstractMessageProcessor() {

            @Override
            public void process0(AioSession session, Object msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        };
        processor1.addPlugin(new MonitorPlugin());

        int serverPort = 8888;

        //启动服务端
        new AioQuickServer<>(serverPort, null, processor1).start();

        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        if (args != null) {
            for (String ip : args) {
                new Thread() {
                    @Override
                    public void run() {
                        int i = 10000;
                        while (i-- > 0) {
                            try {
                                new AioQuickClient(ip, serverPort, null, processor)
                                        .start(channelGroup);
                            } catch (Exception e) {
                                LOGGER.error("exception", e);
                            }
                        }
                    }
                }.start();

            }
        }

    }
}
