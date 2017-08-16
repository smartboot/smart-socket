package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.transport.aio.AioQuickClient;

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
        Properties properties = new Properties();
        properties.put(DetectMessageResp.class.getName(), DetectRespMessageHandler.class.getName());
        P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        try {
            messageFactory.loadFromProperties(properties);
        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        int clientNum = 10;
        P2PClientMessageProcessor[] p2PClientMessageProcessors = new P2PClientMessageProcessor[clientNum];
        AioQuickClient<BaseMessage>[] aioQuickClients = new AioQuickClient[clientNum];
        for (int i = 0; i < clientNum; i++) {
            p2PClientMessageProcessors[i] = new P2PClientMessageProcessor(messageFactory);
            aioQuickClients[i] = new AioQuickClient<BaseMessage>(asynchronousChannelGroup).connect("127.0.0.1", 8888)
                    .setProtocolFactory(new P2PProtocolFactory(messageFactory))
//                    .setFilters(new SmartFilter[]{new QuickMonitorTimer<BaseMessage>()})
                    .setProcessor(p2PClientMessageProcessors[i])
                    .setTimeout(1000);
            aioQuickClients[i].start();
        }

        long num = 0;
        boolean flag = true;
        while (num++ < Long.MAX_VALUE && flag) {
            for (P2PClientMessageProcessor processor : p2PClientMessageProcessors) {
                DetectMessageReq request = new DetectMessageReq();
                request.setSendTime((byte) 1);
                try {
                    processor.getSession().sendWithoutResponse(request);
                } catch (Exception e) {
                    System.out.println(num);
                    e.printStackTrace();
                    flag = false;
                    break;
                }
            }
        }


    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < length; ++i) {
            int number = random.nextInt(62);// [0,62)
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
