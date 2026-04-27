/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TimerTask.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.timer;

public interface TimerTask {
    /**
     * 定时任务是否已执行
     */
    boolean isDone();

    boolean isCancelled();

    /**
     * 取消定时器
     */
    void cancel();
}