package org.smartboot.socket.timer;

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