package org.smartboot.socket.heart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.StringProtocol;
import org.smartboot.socket.extension.plugins.HeartPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class HeartServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartServer.class);

    public static void main(String[] args) throws IOException {
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession<String> session, String msg) {
                LOGGER.info("收到客户端:{}消息：{}", session.getSessionID(), msg);
            }

            @Override
            public void stateEvent0(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                switch (stateMachineEnum) {
                    case SESSION_CLOSED:
                        LOGGER.info("客户端:{} 断开连接", session.getSessionID());
                        break;
                }
            }
        };

        HeartPlugin.TimeoutCallback timeoutCallback = new HeartPlugin.TimeoutCallback() {
            @Override
            public void callback(AioSession session, long lastTime) {
                LOGGER.info("session:{} timeout,last message time:{}", session.getSessionID(), lastTime);
                try {
                    HeartUtil.sendMessage(session,"close_session");
                    session.close(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        processor.addPlugin(new HeartPlugin<String>(1, 5, TimeUnit.SECONDS, timeoutCallback) {
            @Override
            public void sendHeartRequest(AioSession session) throws IOException {
                HeartUtil.sendHeartMessage(session);
            }

            @Override
            public boolean isHeartMessage(AioSession session, String msg) {
                return HeartUtil.isHeartMessage(msg);
            }
        });

        AioQuickServer<String> server = new AioQuickServer<>(8888, new StringProtocol(), processor);
        server.start();
    }
}
