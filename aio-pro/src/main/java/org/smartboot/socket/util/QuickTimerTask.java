/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: QuickTimerTask.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/


package org.smartboot.socket.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 服务器定时任务
 *
 * @author 三刀
 */
public abstract class QuickTimerTask implements Runnable {
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "Quick Timer");
            thread.setDaemon(true);
            return thread;
        }
    });
    private static final Logger logger = LoggerFactory.getLogger(QuickTimerTask.class);

    public QuickTimerTask() {
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this, getDelay(), getPeriod(), TimeUnit.MILLISECONDS);
        logger.info("Regist QuickTimerTask---- " + this.getClass().getSimpleName());
    }

    public static void cancelQuickTask() {
        SCHEDULED_EXECUTOR_SERVICE.shutdown();
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period) {
        return SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(command, initialDelay, period, TimeUnit.MILLISECONDS);
    }


    /**
     * 获取定时任务的延迟启动时间
     */
    protected long getDelay() {
        return 0;
    }

    /**
     * 获取定时任务的执行频率
     *
     * @return
     */
    protected abstract long getPeriod();
}
