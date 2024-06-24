/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BufferPageMonitorPlugin.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioQuickServer;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * 内存页监测插件
 *
 * @author 三刀
 * @version V1.0 , 2019/4/14
 */
public class BufferPageMonitorPlugin<T> extends AbstractPlugin<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPageMonitorPlugin.class);
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
                    LOGGER.error("unKnow server or client need to monitor!");
                    shutdown();
                    return;
                }
                try {
                    Field bufferPoolField = AioQuickServer.class.getDeclaredField("writeBufferPool");
                    bufferPoolField.setAccessible(true);
                    BufferPagePool writeBufferPool = (BufferPagePool) bufferPoolField.get(server);
                    if (writeBufferPool == null) {
                        LOGGER.error("server maybe has not started!");
                        shutdown();
                        return;
                    }
                    Field readBufferPoolField = AioQuickServer.class.getDeclaredField("readBufferPool");
                    readBufferPoolField.setAccessible(true);
                    BufferPagePool readBufferPool = (BufferPagePool) readBufferPoolField.get(server);

                    if (readBufferPool != null && readBufferPool != writeBufferPool) {
                        LOGGER.info("dump writeBuffer");
                        dumpBufferPool(writeBufferPool);
                        LOGGER.info("dump readBuffer");
                        dumpBufferPool(readBufferPool);
                    } else {
                        dumpBufferPool(writeBufferPool);
                    }
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            }
        }, seconds, TimeUnit.SECONDS);
    }

    private static void dumpBufferPool(BufferPagePool writeBufferPool) throws NoSuchFieldException, IllegalAccessException {
        Field field = BufferPagePool.class.getDeclaredField("bufferPages");
        field.setAccessible(true);
        BufferPage[] pages = (BufferPage[]) field.get(writeBufferPool);
        String logger = "";
        for (BufferPage page : pages) {
            logger += "\r\n" + page.toString();
        }
        LOGGER.info(logger);
    }

    private void shutdown() {
        if (future != null) {
            future.cancel();
            future = null;
        }
    }
}
