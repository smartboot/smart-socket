import org.smartboot.socket.transport.AioQuickClient;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * Created by 三刀 on 2017/7/12.
 */
public class BytesClient {
    public static void main(String[] args) throws Exception {
//        fixPackages(1024*1024);
        multiConnect(1024);
    }

    private static void fixPackages(int packageCount)
            throws IOException, ExecutionException, InterruptedException {
        int count = 0;
        BytesClientProcessor processor = new BytesClientProcessor();
        AioQuickClient<byte[]> aioQuickClient = new AioQuickClient<byte[]>("localhost", 8888,
                new BytesProtocol(), processor);
        aioQuickClient.setReadBufferSize(1500);
        aioQuickClient.start();
        long beginMS = new Date().getTime();
        while (count < packageCount) {
            byte[] bytesToWrite = new BytesClient().buildBytesToWrite();
            processor.getSession().write(bytesToWrite);
            count += 1;
            // Thread.sleep(1);
        }

        aioQuickClient.shutdown();
        long endMS = new Date().getTime();
        System.out.println("beginMS = " + beginMS + ",    endMS = " + endMS + ", deltaMS = "
                + (endMS - beginMS));
    }

    private static void multiConnect(int connectCount) throws IOException, ExecutionException, InterruptedException {
        int count = 0;
        long beginMS = new Date().getTime();
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        while (count < connectCount) {
            BytesClientProcessor processor = new BytesClientProcessor();
            AioQuickClient<byte[]> aioQuickClient = new AioQuickClient<byte[]>("localhost", 8888,
                    new BytesProtocol(), processor);
            aioQuickClient.setReadBufferSize(1500);
            aioQuickClient.start(asynchronousChannelGroup);
            byte[] bytesToWrite = new BytesClient().buildBytesToWrite();
            processor.getSession().write(bytesToWrite);
            aioQuickClient.shutdown();
            count += 1;
        }
        long endMS = new Date().getTime();
        System.out.println("beginMS = " + beginMS + ",    endMS = " + endMS + ", deltaMS = "
                + (endMS - beginMS));
    }

    private static boolean isTimeEnd(long timeEndMS) {
        long curMS = new Date().getTime();
        if (curMS > timeEndMS)
            return true;
        return false;
    }

    private static long buildTimeEndMS(int duringSecond) {
        return new Date().getTime() + duringSecond * 1000;
    }

    private byte[] buildBytesToWrite() {
        byte[] bytesToWrite = new byte[1500];
        Random random = new Random();
        random.nextBytes(bytesToWrite);
        return bytesToWrite;
    }

}
