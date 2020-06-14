package org.smartboot.socket.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.StringProtocol;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.BufferPageMonitorPlugin;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class StringServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringServer.class);

    public static void main(String[] args) throws IOException {
        System.setProperty("java.nio.channels.spi.AsynchronousChannelProvider", "org.smartboot.aio.EnhanceAsynchronousChannelProvider");
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession<String> session, String msg) {
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
            public void stateEvent0(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    LOGGER.error(stateMachineEnum + " exception:", throwable);
                }
            }
        };

        BufferPagePool bufferPagePool = new BufferPagePool(1024 * 1024 * 16, Runtime.getRuntime().availableProcessors() + 1, true);
        AioQuickServer<String> server = new AioQuickServer<>(8888, new StringProtocol(), processor);
        server.setReadBufferSize(1024 * 1024)
                .setThreadNum(Runtime.getRuntime().availableProcessors() + 1)
                .setBufferFactory(() -> bufferPagePool)
                .setWriteBuffer(4096, 512);
        processor.addPlugin(new BufferPageMonitorPlugin(server, 6));
        processor.addPlugin(new MonitorPlugin(5));
        server.start();

    }
}
