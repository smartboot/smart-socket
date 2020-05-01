/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WheelTimer.java
 * Date: 2020-04-30
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.util;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/30
 */
public class WheelTimer {
    private ConcurrentLinkedQueue<Task>[] wheel;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    /**
     * 频率，默认：1秒
     */
    private long period;
    private int wheelCount;
    private int currentIndex = 0;
    private long startTime;

    public WheelTimer(long period, int wheelCount) {
        this.period = period;
        this.wheelCount = wheelCount;
        this.wheel = new ConcurrentLinkedQueue[wheelCount];
        for (int i = 0; i < wheelCount; i++) {
            this.wheel[i] = new ConcurrentLinkedQueue<>();
        }
        new Thread(() -> WheelTimer.this.run(), "WheelTimer").start();
    }

    public static void main(String[] args) {
        WheelTimer timer = new WheelTimer(1000, 10);
        int count = 1000;

        while (count-- > 0) {
            long expectTime = System.currentTimeMillis() + (long) (Math.random() * 100) * 1000;
            timer.put(new Runnable() {
                @Override
                public void run() {
                    System.out.println("误差:" + (System.currentTimeMillis() - expectTime));
                }
            }, expectTime);
        }
    }

    public void put(Runnable runnable, long time) {
        int cycls = (int) ((time - System.currentTimeMillis()) / period);
        if (cycls <= 0) {
            executorService.execute(runnable);
        } else {
            int index = (cycls + currentIndex) % wheelCount;
            wheel[index].add(new Task(runnable, time));
        }
    }

    private void run() {
        startTime = System.currentTimeMillis();
        long next = startTime + period;
        while (true) {
            long waitTime = next - System.currentTimeMillis();
            if (waitTime > 0) {
                synchronized (this) {
                    try {
                        this.wait(waitTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Iterator<Task> iterator = wheel[currentIndex++ % wheelCount].iterator();
            Task task = null;
            long current = System.currentTimeMillis();
            while (iterator.hasNext()) {
                task = iterator.next();
                //已到执行时间
//                if (task.executeTime < current || (task.executeTime - current) < (period >> 1)) {
                if (task.executeTime < current) {
                    iterator.remove();
                    executorService.execute(task.task);
                }
                // wheelCount * period 时间内存在待执行任务,矫正执行节点
                else if ((current + wheelCount * period) > task.executeTime) {
                    iterator.remove();
                    put(task.task, task.executeTime);
                }
            }

            next += period;
        }
    }

    class Task {
        private final long executeTime;
        private Runnable task;

        public Task(Runnable task, long executeTime) {
            this.task = task;
            this.executeTime = executeTime;
        }
    }
}
