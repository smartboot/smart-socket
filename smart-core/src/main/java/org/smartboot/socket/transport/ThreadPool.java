package org.smartboot.socket.transport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Seer
 * @version V1.0 , 2017/9/8
 */
public class ThreadPool {
    private static ExecutorService executor = Executors.newFixedThreadPool(8);

    public static ExecutorService getThreadPool() {
        return executor;
    }
}
