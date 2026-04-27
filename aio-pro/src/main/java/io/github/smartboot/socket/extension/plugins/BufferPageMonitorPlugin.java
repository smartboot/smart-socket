/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BufferPageMonitorPlugin.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.extension.plugins;

import io.github.smartboot.socket.Plugin;
import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.timer.HashedWheelTimer;
import io.github.smartboot.socket.timer.TimerTask;
import io.github.smartboot.socket.transport.AioQuickServer;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * 内存页监测插件
 *
 * @author 三刀
 * @version V1.0 , 2019/4/14
 */
public class BufferPageMonitorPlugin<T> implements Plugin<T> {
    /**
     * 任务执行频率
     */
    private int seconds = 0;

    private AioQuickServer server;

    private TimerTask future;

    public BufferPageMonitorPlugin(AioQuickServer server, int seconds) {
        this.seconds = seconds;
        this.server = server;
        init();
    }

    private void init() {
        future = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
            {
                if (server == null) {
                    System.err.println("unKnow server or client need to monitor!");
                    shutdown();
                    return;
                }
                try {
                    Field bufferPoolField = AioQuickServer.class.getDeclaredField("writeBufferPool");
                    bufferPoolField.setAccessible(true);
                    BufferPagePool writeBufferPool = (BufferPagePool) bufferPoolField.get(server);
                    if (writeBufferPool == null) {
                        System.err.println("server maybe has not started!");
                        shutdown();
                        return;
                    }
                    Field readBufferPoolField = AioQuickServer.class.getDeclaredField("readBufferPool");
                    readBufferPoolField.setAccessible(true);
                    BufferPagePool readBufferPool = (BufferPagePool) readBufferPoolField.get(server);

                    if (readBufferPool != null && readBufferPool != writeBufferPool) {
                        dumpBufferPool(writeBufferPool);
                        dumpBufferPool(readBufferPool);
                    } else {
                        dumpBufferPool(writeBufferPool);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, seconds, TimeUnit.SECONDS);
    }

    private static void dumpBufferPool(BufferPagePool writeBufferPool) {
        System.out.println(writeBufferPool);
    }

    private void shutdown() {
        if (future != null) {
            future.cancel();
            future = null;
        }
    }
}
