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
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.util.QuickTimerTask;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledFuture;
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

    private ScheduledFuture<?> future;

    public BufferPageMonitorPlugin(AioQuickServer server, int seconds) {
        this.seconds = seconds;
        this.server = server;
        init();
    }

    private void init() {
        long mills = TimeUnit.SECONDS.toMillis(seconds);
        future = QuickTimerTask.scheduleAtFixedRate(() -> {
            {
                if (server == null) {
                    LOGGER.error("unKnow server or client need to monitor!");
                    shutdown();
                    return;
                }
                try {
                    Field bufferPoolField = AioQuickServer.class.getDeclaredField("bufferPool");
                    bufferPoolField.setAccessible(true);
                    BufferPagePool pagePool = (BufferPagePool) bufferPoolField.get(server);
                    if (pagePool == null) {
                        LOGGER.error("server maybe has not started!");
                        shutdown();
                        return;
                    }
                    Field field = BufferPagePool.class.getDeclaredField("bufferPages");
                    field.setAccessible(true);
                    BufferPage[] pages = (BufferPage[]) field.get(pagePool);
                    String logger = "";
                    for (BufferPage page : pages) {
                        logger += "\r\n" + page.toString();
                    }
                    LOGGER.info(logger);
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            }
        }, mills, mills);
    }

    private void shutdown() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }
}
