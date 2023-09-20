package org.smartboot.socket.timer;

import java.util.concurrent.TimeUnit;

public interface Timer {

    TimerTask schedule(final Runnable runnable, final long delay, final TimeUnit unit);

    void shutdown();

    TimerTask scheduleWithFixedDelay(Runnable runnable, long delay, TimeUnit unit);
}