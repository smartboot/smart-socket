package org.smartboot.socket.protocol.p2p.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.protocol.p2p.P2PProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.DetectMessageResp;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.service.filter.SmartFilter;
import org.smartboot.socket.transport.AioQuickClient;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadFactory;

public class P2PMultiClient {
    public static void main(String[] args) throws Exception {
        final AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        for (int i = 0; i < 10; i++) {
            new Thread("CLient-Thread-" + i) {
                private Logger logger = LogManager.getLogger(this.getClass());

                @Override
                public void run() {
                    Properties properties = new Properties();
                    properties.put(DetectMessageResp.class.getName(), DetectRespMessageHandler.class.getName());
                    P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
                    try {
                        messageFactory.loadFromProperties(properties);
                    } catch (ClassNotFoundException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    P2PClientMessageProcessor processor = new P2PClientMessageProcessor(messageFactory);
                    AioQuickClient<BaseMessage> client = new AioQuickClient<BaseMessage>().connect("127.0.0.1", 8888)
                            .setProtocol(new P2PProtocol(messageFactory))
                            .setFilters(new SmartFilter[]{new QuickMonitorTimer<BaseMessage>()})
                            .setProcessor(processor);
                    try {
                        client.start(asynchronousChannelGroup);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    long num = 0;
                    long start = System.currentTimeMillis();
                    while (num++ < Long.MAX_VALUE) {
                        DetectMessageReq request = new DetectMessageReq();
                        request.setSendTime((byte) 1);
                        try {
                            processor.getSession().sendWithoutResponse(request);
                        } catch (Exception e) {
                            System.out.println(num);
                            e.printStackTrace();
                            break;
                        }
                    }
                    logger.info("安全消息结束" + (System.currentTimeMillis() - start));
                    client.shutdown();
                }

            }.start();
        }

    }
}
