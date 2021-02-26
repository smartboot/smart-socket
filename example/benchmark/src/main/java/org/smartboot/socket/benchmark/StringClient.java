package org.smartboot.socket.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.StringProtocol;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class StringClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringClient.class);


    public static void main(String[] args) throws IOException {
        System.setProperty("java.nio.channels.spi.AsynchronousChannelProvider", "org.smartboot.aio.EnhanceAsynchronousChannelProvider");
        BufferPagePool bufferPagePool = new BufferPagePool(1024 * 1024 * 32, 10, true);
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        };
        processor.addPlugin(new MonitorPlugin(5));
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "ClientGroup");
            }
        });
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        new StringClient().test(asynchronousChannelGroup, bufferPagePool, processor);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }.start();
        }

    }

    public void test(AsynchronousChannelGroup asynchronousChannelGroup, BufferPagePool bufferPagePool, AbstractMessageProcessor<String> processor) throws InterruptedException, ExecutionException, IOException {
        AioQuickClient client = new AioQuickClient("localhost", 8888, new StringProtocol(), processor);
        client.setBufferPagePool(bufferPagePool);
        client.setWriteBuffer(1024 * 1024, 10);
        AioSession session = client.start(asynchronousChannelGroup);
        WriteBuffer outputStream = session.writeBuffer();

        byte[] data = "smart-socket".getBytes();
        while (true) {
            int num = (int) (Math.random() * 10) + 1;
//            int num = 4;
            outputStream.writeInt(data.length * num);
            while (num-- > 0) {
                outputStream.write(data);
            }

//            Thread.sleep(100);
        }
    }
}
