package org.smartboot.socket.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * ulimit -n 1000000
 * sysctl -w kern.maxfilesperproc=1000000
 *
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("PORT", "8080"));
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
//                LOGGER.info(msg);
                WriteBuffer outputStream = session.writeBuffer();

                try {
                    byte[] bytes = msg.getBytes();
                    outputStream.writeInt(bytes.length);
                    outputStream.write(bytes);
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    LOGGER.error(stateMachineEnum + " exception:", throwable);
                }
            }
        };

        BufferPagePool bufferPagePool = new BufferPagePool(1024 * 1024, Runtime.getRuntime().availableProcessors() + 1, true);
        AioQuickServer server = new AioQuickServer(port, new StringProtocol(), processor);
        server.setReadBufferSize(1024)
                .setThreadNum(Runtime.getRuntime().availableProcessors() + 1)
                .setBufferPagePool(bufferPagePool)
                .disableLowMemory()
                .setWriteBuffer(4096, 1);
        processor.addPlugin(new MonitorPlugin<>(5));
        server.start();

    }
}
