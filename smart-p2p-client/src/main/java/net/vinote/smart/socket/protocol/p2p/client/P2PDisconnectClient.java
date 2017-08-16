package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.QuickMonitorTimer;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.transport.aio.AioQuickClient;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;

public class P2PDisconnectClient {
    public static void main(String[] args) throws Exception {
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
        final ArrayBlockingQueue<AioQuickClient<BaseMessage>> aioQuickClientArrayBlockingQueue = new ArrayBlockingQueue<AioQuickClient<BaseMessage>>(1024);
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    AioQuickClient<BaseMessage> client = new AioQuickClient<BaseMessage>(asynchronousChannelGroup).connect("127.0.0.1", 8888)
                            .setProtocolFactory(new P2PProtocolFactory(messageFactory))
//                            .setFilters(new SmartFilter[]{new QuickMonitorTimer<BaseMessage>()})
                            .setProcessor(new P2PClientMessageProcessor(messageFactory))
                            .setTimeout(1000);

                    try {
                        client.start();
                        aioQuickClientArrayBlockingQueue.put(client);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        aioQuickClientArrayBlockingQueue.take().shutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();


    }


}
