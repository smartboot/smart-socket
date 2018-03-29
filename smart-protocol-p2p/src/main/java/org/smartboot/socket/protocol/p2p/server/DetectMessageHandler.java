package org.smartboot.socket.protocol.p2p.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.protocol.p2p.MessageHandler;
import org.smartboot.socket.protocol.p2p.Session;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.DetectMessageResp;

import java.io.IOException;

/**
 * 探测消息处理器
 *
 * @author 三刀
 */
public class DetectMessageHandler extends MessageHandler {
    private static Logger logger = LoggerFactory.getLogger(DetectMessageHandler.class);

    @Override
    public void handler(Session<BaseMessage> session, BaseMessage message) {
//		logger.info("收到消息");
        DetectMessageReq msg = (DetectMessageReq) message;
        DetectMessageResp rspMsg = new DetectMessageResp(msg.getHead());
        rspMsg.setSendTime(System.currentTimeMillis());
        try {
            session.sendWithoutResponse(rspMsg);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
