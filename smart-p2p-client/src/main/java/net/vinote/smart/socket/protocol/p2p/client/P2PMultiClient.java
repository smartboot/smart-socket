package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.extension.timer.QuickMonitorTimer;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.transport.nio.NioQuickClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.Random;

public class P2PMultiClient {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10; i++) {
            new Thread() {
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
                    NioQuickClient<BaseMessage> client = new NioQuickClient<BaseMessage>().connect("127.0.0.1", 8888)
                            .setProtocolFactory(new P2PProtocolFactory(messageFactory))
                            .setFilters(new SmartFilter[]{new QuickMonitorTimer<BaseMessage>()})
                            .setProcessor(processor)
                            .setTimeout(1000);
                    client.start();

                    long num = 0;
                    long start = System.currentTimeMillis();
                    while (num++ < Long.MAX_VALUE) {
                        DetectMessageReq request = new DetectMessageReq();
                        request.setSendTime((byte) 1);
                        try {
//							DetectMessageResp loginResp = (DetectMessageResp) processor.getSession()
//								.sendWithResponse(request);
                            processor.getSession().sendWithoutResponse(request);
                            // logger.info(loginResp);
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
            Thread.sleep(500);
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
