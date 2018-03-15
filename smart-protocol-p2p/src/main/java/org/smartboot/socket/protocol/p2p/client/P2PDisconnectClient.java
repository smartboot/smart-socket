package org.smartboot.socket.protocol.p2p.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Filter;
import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.protocol.p2p.P2PProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.DetectMessageResp;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.transport.AioQuickClient;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

public class P2PDisconnectClient {
    public static void main(String[] args) throws Exception {
        final Logger logger = LogManager.getLogger(P2PDisconnectClient.class);
        final AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        Properties properties = new Properties();
        properties.put(DetectMessageResp.class.getName(), DetectRespMessageHandler.class.getName());
        final P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        try {
            messageFactory.loadFromProperties(properties);
        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        AioQuickClient<BaseMessage> client = null;
                        try {
                            P2PClientMessageProcessor processor = new P2PClientMessageProcessor(messageFactory);
                            client = new AioQuickClient<BaseMessage>("127.0.0.1", 8888, new P2PProtocol(messageFactory), processor);
                            client.start(asynchronousChannelGroup);
                            long num = 0;
                            while (num++ < 10) {
                                DetectMessageReq request = new DetectMessageReq();
                                request.setDetect("台州人在杭州:" + num);
                                try {
//                                    processor.getSession().sendWithoutResponse(request);
                                    logger.info(processor.getSession().sendWithResponse(request, 0));
//                                    Thread.sleep(100);
                                } catch (Exception e) {
                                    System.out.println(num);
                                    e.printStackTrace();
//                            break;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            if (client != null) {
                                client.shutdown();
                            }
                        }
                    }
                }
            }).start();
        }

        for (int i = 0; i < 10; i++) {
            new Thread("Client-Thread-" + i) {
                @Override
                public void run() {
                    Properties properties = new Properties();
                    properties.put(DetectMessageResp.class.getName(), DetectRespMessageHandler.class.getName());
                    P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
                    try {
                        messageFactory.loadFromProperties(properties);
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                    P2PClientMessageProcessor processor = new P2PClientMessageProcessor(messageFactory);
                    AioQuickClient<BaseMessage> client = new AioQuickClient<BaseMessage>("127.0.0.1", 8888, new P2PProtocol(messageFactory), processor);
                    client.setFilters(new Filter[]{new QuickMonitorTimer<BaseMessage>()})
                            .setWriteQueueSize(16384);
                    try {
                        client.start(asynchronousChannelGroup);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    long num = 0;
                    long start = System.currentTimeMillis();
                    while (num++ < Integer.MAX_VALUE) {
                        DetectMessageReq request = new DetectMessageReq();
                        request.setDetect("台州人在杭州:" + num);
                        try {
                            logger.info(processor.getSession().sendWithResponse(request, 0));
                            Thread.sleep(100);
                        } catch (Exception e) {
                            System.out.println(num);
                            e.printStackTrace();
                        }
                    }
                    logger.info("安全消息结束" + (System.currentTimeMillis() - start));
                    client.shutdown();
                }

            }.start();
        }
    }


}
