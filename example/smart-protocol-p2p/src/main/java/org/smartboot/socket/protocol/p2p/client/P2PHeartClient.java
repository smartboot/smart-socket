package org.smartboot.socket.protocol.p2p.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.extension.plugins.HeartPlugin;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.protocol.p2p.P2PProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.HeartMessageReq;
import org.smartboot.socket.protocol.p2p.message.HeartMessageRsp;
import org.smartboot.socket.protocol.p2p.message.MessageType;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

public class P2PHeartClient {
    public static void main(String[] args) throws Exception {
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
                    properties.put(HeartMessageReq.class.getName(), "");
                    properties.put(HeartMessageRsp.class.getName(), "");
                    P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
                    try {
                        messageFactory.loadFromProperties(properties);
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                    P2PClientMessageProcessor processor = new P2PClientMessageProcessor(messageFactory);
                    processor.addPlugin(new MonitorPlugin());
                    processor.addPlugin(new HeartPlugin<BaseMessage>(-1) {
                        @Override
                        public void sendHeartRequest(AioSession<BaseMessage> session) throws IOException {

                        }

                        @Override
                        public boolean isHeartMessage(AioSession<BaseMessage> session, BaseMessage msg) {
                            if (msg.getMessageType() == MessageType.HEART_MESSAGE_RSP || msg.getMessageType() == MessageType.HEART_MESSAGE_REQ) {
                                System.out.println("收到心跳消息");
                                return true;
                            }
                            return false;
                        }
                    });
                    AioQuickClient<BaseMessage> client = new AioQuickClient<BaseMessage>("localhost", 8888, new P2PProtocol(messageFactory), processor);
                    try {
                        client.start(asynchronousChannelGroup);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }.start();
        }

    }
}
