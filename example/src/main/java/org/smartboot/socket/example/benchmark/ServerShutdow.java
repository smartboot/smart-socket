package org.smartboot.socket.example.benchmark;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import org.smartboot.socket.extension.plugins.BufferPageMonitorPlugin;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class ServerShutdow {

    public static void main(String[] args) throws IOException, InterruptedException {
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                WriteBuffer outputStream = session.writeBuffer();

                try {
                    byte[] bytes = msg.getBytes();
                    outputStream.writeInt(bytes.length);
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
            }
        };

        BufferPagePool bufferPagePool = new BufferPagePool(1024 * 1024 * 16, Runtime.getRuntime().availableProcessors() + 1, true);
        AioQuickServer server = new AioQuickServer(8888, new StringProtocol(), processor);
        server.setReadBufferSize(1024 * 1024)
                .setThreadNum(2)
                .setBufferPagePool(bufferPagePool)
                .disableLowMemory()
                .setWriteBuffer(4096, 512);
        processor.addPlugin(new BufferPageMonitorPlugin<>(server, 6));
        processor.addPlugin(new MonitorPlugin<>(5));
        server.start();

        AsynchronousChannelGroup asynchronousChannelGroup = new EnhanceAsynchronousChannelProvider(false).openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), r -> new Thread(r, "ClientGroup"));
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                try {
                    test(asynchronousChannelGroup, bufferPagePool, processor);
                } catch (Throwable e) {
                    e.printStackTrace();
                }

            }).start();
        }
        System.out.println("start sleep:" + new Date());
        Thread.sleep(10000);
        System.out.println("end sleep:" + new Date());
        server.shutdown();
        Thread.sleep(Integer.MAX_VALUE);
    }

    public static void test(AsynchronousChannelGroup asynchronousChannelGroup, BufferPagePool bufferPagePool, AbstractMessageProcessor<String> processor) throws InterruptedException, ExecutionException, IOException {
        AioQuickClient client = new AioQuickClient("localhost", 8888, new StringProtocol(), processor);
        client.setBufferPagePool(bufferPagePool);
        client.setWriteBuffer(1024 * 1024, 10);
        AioSession session = client.start(asynchronousChannelGroup);
        WriteBuffer outputStream = session.writeBuffer();

        byte[] data = "smart-socket".getBytes();
        while (true) {
            int num = (int) (Math.random() * 10) + 1;
            outputStream.writeInt(data.length * num);
            while (num-- > 0) {
                outputStream.write(data);
            }
        }
    }
}
