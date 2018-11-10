package org.smartboot.socket.protocol.p2p.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.protocol.p2p.P2PProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.DetectMessageResp;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.transport.AioQuickClient;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

public class P2PMultiClient {
    public static void main(String[] args) throws Exception {
//        System.setProperty("javax.net.debug", "ssl");
        final AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        for (int i = 0; i < 10; i++) {
            new Thread("Client-Thread-" + i) {
                private Logger logger = LoggerFactory.getLogger(this.getClass());

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
                    processor.addPlugin(new MonitorPlugin());
//                    AioSSLQuickClient<BaseMessage> client = new AioSSLQuickClient<BaseMessage>("127.0.0.1", 9222, new P2PProtocol(messageFactory), processor);
//                    client.setKeyStore("client.jks", "storepass")
//                            .setTrust("trustedCerts.jks", "storepass")
//                            .setKeyPassword("keypass")
//                            .setWriteQueueSize(16384);
                    AioQuickClient<BaseMessage> client = new AioQuickClient<BaseMessage>("localhost", 8888, new P2PProtocol(messageFactory), processor);
                    client
//                            .setDirectBuffer(true)
                            .setWriteQueueSize(16384)
                    ;
                    try {
                        client.start(asynchronousChannelGroup);
//                        Thread.sleep(4000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    long num = 0;
                    long start = System.currentTimeMillis();
                    while (num++ < Integer.MAX_VALUE) {
                        DetectMessageReq request = new DetectMessageReq();
                        request.setDetect("台州人在杭州:" + num);
                        try {
                            processor.getSession().sendWithoutResponse(request);
//                            logger.info(processor.getSession().sendWithResponse(request, 0).toString());
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
