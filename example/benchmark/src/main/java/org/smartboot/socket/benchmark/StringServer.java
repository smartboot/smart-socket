package org.smartboot.socket.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.BufferOutputStream;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class StringServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringServer.class);

    public static void main(String[] args) throws IOException {
        System.setProperty("smart-socket.server.pageSize", (1024 * 1024 * 16) + "");
        System.setProperty("smart-socket.session.writeChunkSize", "2048");
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession<String> session, String msg) {
//                LOGGER.info(msg);
                BufferOutputStream outputStream = session.getOutputStream();
                byte[] bytes = msg.getBytes();
                outputStream.writeInt(bytes.length);
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void stateEvent0(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        };
        processor.addPlugin(new MonitorPlugin());

        AioQuickServer<String> server = new AioQuickServer<>(8888, new StringProtocol(), processor);

        server.start();
    }
}
