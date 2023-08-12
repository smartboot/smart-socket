package org.smartboot.socket.timer;

public interface TimerTask {
    /**
     * 定时任务是否已执行
     */
    boolean isExecuted();

    boolean isCancelled();

    /**
     * 取消定时器
     */
    boolean cancel();
}