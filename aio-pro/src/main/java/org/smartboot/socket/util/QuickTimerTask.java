/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: QuickTimerTask.java
 * Date: 2017-11-25
 * Author: sandao
 */


package org.smartboot.socket.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 服务器定时任务
 *
 * @author 三刀
 */
public abstract class QuickTimerTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(QuickTimerTask.class);
    private static Timer timer = new Timer("Quick Timer", true);

    public QuickTimerTask() {
        timer.schedule(this, getDelay(), getPeriod());
        logger.info("Regist QuickTimerTask---- " + this.getClass().getSimpleName());
    }

    public static void cancelQuickTask() {
        timer.cancel();
    }

    /**
     * 获取定时任务的延迟启动时间
     */
    protected long getDelay() {
        return 0;
    }

    /**
     * 获取定时任务的执行频率
     */
    protected abstract long getPeriod();
}
