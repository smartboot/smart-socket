package org.smartboot.socket.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class StringClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringClient.class);
    AsynchronousChannelGroup asynchronousChannelGroup;

    public StringClient(AsynchronousChannelGroup asynchronousChannelGroup) {
        this.asynchronousChannelGroup = asynchronousChannelGroup;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        System.setProperty("smart-socket.server.pageSize", (1024 * 1024 * 32) + "");
        System.setProperty("smart-socket.session.writeChunkSize", "2048");
        final AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });

        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        new StringClient(asynchronousChannelGroup).test();
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

    public void test() throws InterruptedException, ExecutionException, IOException {
        AioQuickClient<String> client = new AioQuickClient<>("localhost", 8888, new StringProtocol(), new MessageProcessor<String>() {
            @Override
            public void process(AioSession<String> session, String msg) {
//                LOGGER.info(msg);
            }

            @Override
            public void stateEvent(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        });
        client.setWriteQueueSize(16384);
        AioSession<String> session = client.start(asynchronousChannelGroup);

        int i = 1;
        while (true) {
            int num = (int) (Math.random() * 10) + 1;
            StringBuilder sb = new StringBuilder();
            while (num-- > 0) {
                sb.append("smart-socket");
            }
            byte[] bytes = sb.toString().getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            session.write(buffer);
//            outputStream.writeInt(bytes.length);
//            outputStream.write(bytes);
        }
    }
}
