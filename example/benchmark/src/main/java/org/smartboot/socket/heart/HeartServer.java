package org.smartboot.socket.heart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.StringProtocol;
import org.smartboot.socket.extension.plugins.HeartPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class HeartServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartServer.class);

    public static void main(String[] args) throws IOException {
        //定义消息处理器
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

        //注册心跳插件：每隔1秒发送一次心跳请求，5秒内未收到消息超时关闭连接
        processor.addPlugin(new HeartPlugin<String>(1, 5, TimeUnit.SECONDS) {
            @Override
            public void sendHeartRequest(AioSession session) throws IOException {
                WriteBuffer writeBuffer = session.writeBuffer();
                byte[] heartBytes = "heart_req".getBytes();
                writeBuffer.writeInt(heartBytes.length);
                writeBuffer.write(heartBytes);
                writeBuffer.flush();
                LOGGER.info("发送心跳请求至客户端:{}", session.getSessionID());
            }

            @Override
            public boolean isHeartMessage(AioSession session, String msg) {
                //心跳请求消息,返回响应
                if ("heart_req".equals(msg)) {
                    try {
                        WriteBuffer writeBuffer = session.writeBuffer();
                        byte[] heartBytes = "heart_rsp".getBytes();
                        writeBuffer.writeInt(heartBytes.length);
                        writeBuffer.write(heartBytes);
                        writeBuffer.flush();
                    } catch (Exception e) {
                    }
                    return true;
                }
                //是否为心跳响应消息
                if ("heart_rsp".equals(msg)) {
                    LOGGER.info("收到来自客户端:{} 的心跳响应消息", session.getSessionID());
                    return true;
                }
                return false;
            }
        });

        //启动服务
        AioQuickServer<String> server = new AioQuickServer<>(8888, new StringProtocol(), processor);
        server.start();
    }
}
