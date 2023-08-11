package org.smartboot.socket.timer;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface Timer {

    Timeout newTimeout(final Runnable runnable, final long delay, final TimeUnit unit);

    Set<Timeout> stop();
}