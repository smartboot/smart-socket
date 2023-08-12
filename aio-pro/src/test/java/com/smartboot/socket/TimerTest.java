package com.smartboot.socket;

import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.Timer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TimerTest {
    public static void main(String[] args) throws InterruptedException {
        Timer timer = new HashedWheelTimer(Thread::new);
        int i = 5000000;
        CountDownLatch countDownLatch = new CountDownLatch(i);
        while (i-- > 0) {
            timer.schedule(new Runnable() {
                @Override
                public void run() {
                    countDownLatch.countDown();
                }
            }, 1 + ((int) (Math.random() * 1000)) % 10, TimeUnit.SECONDS);
        }
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> System.out.println(countDownLatch.getCount()), 1, 1, TimeUnit.SECONDS);
        countDownLatch.await();
        System.out.println("success");
    }
}
