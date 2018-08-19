package org.smartboot.socket.protocol.p2p.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.protocol.p2p.P2PProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
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
        final Logger logger = LoggerFactory.getLogger(P2PDisconnectClient.class);
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

                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
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
    }


}
