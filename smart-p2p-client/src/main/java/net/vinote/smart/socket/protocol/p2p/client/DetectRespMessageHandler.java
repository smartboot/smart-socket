package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.protocol.p2p.MessageHandler;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageResp;
import net.vinote.smart.socket.protocol.p2p.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 探测消息处理器
 *
 * @author Seer
 */
public class DetectRespMessageHandler extends MessageHandler {
    private static Logger logger = LogManager.getLogger(DetectRespMessageHandler.class);
    long max = 0;
    long outTime = System.currentTimeMillis();

    @Override
    public void handler(Session<BaseMessage> session, BaseMessage message) {
//        System.out.println("handler message:"+message);
        DetectMessageResp msg = (DetectMessageResp) message;
        long useTime = System.currentTimeMillis() - msg.getSendTime();
        if (useTime > max) {
            max = useTime;
            if ( (System.currentTimeMillis() - outTime) > 1000) {
                outTime = System.currentTimeMillis();
//                System.out.println(max);
            }
        }

    }
}
