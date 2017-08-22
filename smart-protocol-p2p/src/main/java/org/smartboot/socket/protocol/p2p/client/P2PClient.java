package org.smartboot.socket.protocol.p2p.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.P2PProtocolFactory;
import org.smartboot.socket.protocol.p2p.QuickMonitorTimer;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.DetectMessageResp;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.service.filter.SmartFilter;
import org.smartboot.socket.transport.AioQuickClient;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

public class P2PClient {
    public static void main(String[] args) throws Exception {
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        Properties properties = new Properties();
        properties.put(DetectMessageResp.class.getName(), DetectRespMessageHandler.class.getName());
        P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        try {
            messageFactory.loadFromProperties(properties);
        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        final P2PClientMessageProcessor processor = new P2PClientMessageProcessor(messageFactory);
        final AioQuickClient<BaseMessage> client = new AioQuickClient<BaseMessage>(asynchronousChannelGroup).connect("127.0.0.1", 8888)
                .setProtocolFactory(new P2PProtocolFactory(messageFactory))
//                .setProcessor(processor)
                .setFilters(new SmartFilter[]{new QuickMonitorTimer<BaseMessage>()})
                .setTimeout(1000);
        client.start();

        for (int i = 0; i < 1; i++) {
            new Thread() {
                private Logger logger = LogManager.getLogger(this.getClass());

                @Override
                public void run() {

                    long num = 0;
                    long start = System.currentTimeMillis();
                    while (num++ < Long.MAX_VALUE) {
                        DetectMessageReq request = new DetectMessageReq();
                        try {
//							DetectMessageResp loginResp = (DetectMessageResp) processor.getSession()
//								.sendWithResponse(request);
                            System.out.println(processor.getSession().sendWithResponse(request));
//							 Thread.sleep(1);
                            // logger.info(loginResp);
                        } catch (Exception e) {
                            System.out.println(num);
                            e.printStackTrace();
//							System.exit(0);
                        }
                    }
                    logger.info("安全消息结束" + (System.currentTimeMillis() - start));
                    client.shutdown();
                }

            }.start();
            Thread.sleep(500);
        }

    }
}
