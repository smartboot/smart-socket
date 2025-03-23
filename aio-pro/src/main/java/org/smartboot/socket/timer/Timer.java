package org.smartboot.socket.timer;

import java.util.concurrent.TimeUnit;

/**
 * 定时器接口，提供定时任务的调度功能
 * 
 * @author 三刀
 */
public interface Timer {

    /**
     * 调度一个延迟执行的任务
     * 
     * @param runnable 待执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return 定时任务对象，可用于取消任务
     */
    TimerTask schedule(final Runnable runnable, final long delay, final TimeUnit unit);

    /**
     * 关闭定时器，停止所有未执行的定时任务
     */
    void shutdown();

    /**
     * 调度一个固定延迟执行的任务
     * 
     * @param runnable 待执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return 定时任务对象，可用于取消任务
     */
    TimerTask scheduleWithFixedDelay(Runnable runnable, long delay, TimeUnit unit);
}