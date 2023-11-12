package com.smartboot.socket;

import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.Timer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerTest {
    public static void main(String[] args) throws InterruptedException {
        Timer timer = new HashedWheelTimer(Thread::new);
        int i = 100000;
        AtomicInteger count = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(i);
        while (i-- > 0) {
            int delay = ((int) (Math.random() * 1000)) % 10 + ((int) (Math.random() * 1000)) % 10;
            long endTime = System.currentTimeMillis() + delay;
            timer.schedule(new Runnable() {
                @Override
                public void run() {
                    long delay = System.currentTimeMillis() - endTime;
                    if (delay > 100) {
                        System.out.println(count.incrementAndGet() + ": " + delay);
                    }
                    countDownLatch.countDown();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
//        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> System.out.println(countDownLatch.getCount()), 1, 1, TimeUnit.SECONDS);
        countDownLatch.await();
        System.out.println("success");
    }
}
