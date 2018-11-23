package org.smartboot.socket.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.BufferOutputStream;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class StringClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringClient.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        System.setProperty("smart-socket.server.pageSize", (1024 * 1024 * 4) + "");
        System.setProperty("smart-socket.session.writeChunkSize", "2048");
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        new StringClient().test();
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

            }
        });
        AioSession<String> session = client.start();
        Thread.sleep(5000);
        BufferOutputStream outputStream = session.getOutputStream();

        int i = 1;
        while (true) {
            int num = (int) (Math.random() * 10) + 1;
            StringBuilder sb = new StringBuilder();
            while (num-- > 0) {
                sb.append("smart-socket");
            }
            byte[] bytes = sb.toString().getBytes();
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
        }
    }
}
