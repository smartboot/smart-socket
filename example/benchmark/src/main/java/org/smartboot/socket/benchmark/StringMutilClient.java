package org.smartboot.socket.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class StringMutilClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringMutilClient.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        System.setProperty("smart-socket.session.writeChunkSize", "" + (1024 * 1024));

        BufferPagePool bufferPagePool = new BufferPagePool(1024 * 1024 * 32, 10, true);
        AbstractMessageProcessor processor = new AbstractMessageProcessor() {
            @Override
            public void process0(AioSession session, Object msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        };
//        processor.addPlugin(new HeartPlugin(3000) {
//            @Override
//            public void sendHeartRequest(AioSession session) throws IOException {
//
//            }
//
//            @Override
//            public boolean isHeartMessage(AioSession session, Object msg) {
//                return true;
//            }
//        });
        AioQuickClient<String> client = new AioQuickClient<>("localhost", 8888, new StringProtocol(), processor);
        client.setBufferPagePool(bufferPagePool);
        client.setWriteQueueCapacity(20);
        AioSession<String> session = client.start();
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        WriteBuffer outputStream = session.writeBuffer();
                        byte[] data = "smart-s1ocket".getBytes();
                        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + data.length);
                        buffer.putInt(data.length);
                        buffer.put(data);
                        byte[] a = buffer.array();
                        while (true) {

                            outputStream.write(a);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }.start();
        }

    }

}
