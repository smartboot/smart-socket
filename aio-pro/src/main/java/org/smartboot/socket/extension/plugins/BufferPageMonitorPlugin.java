package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.util.QuickTimerTask;

import java.lang.reflect.Field;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * 内存页监测插件
 *
 * @author 三刀
 * @version V1.0 , 2019/4/14
 */
public class BufferPageMonitorPlugin<T> extends AbstractPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPageMonitorPlugin.class);
    /**
     * 任务执行频率
     */
    private int seconds = 0;

    private AioQuickClient<T> client;

    private AioQuickServer<T> server;

    public BufferPageMonitorPlugin(AioQuickClient<T> client, int seconds) {
        this.seconds = seconds;
        this.client = client;
        init();
    }

    public BufferPageMonitorPlugin(AioQuickServer<T> server, int seconds) {
        this.seconds = seconds;
        this.server = server;
        init();
    }

    private void init() {
        long mills = TimeUnit.SECONDS.toMillis(seconds);
        QuickTimerTask.getTimer().schedule(new TimerTask() {
            @Override
            public void run() {
                {
                    if (client == null && server == null) {
                        LOGGER.error("unKnow server or client need to monitor!");
                        return;
                    }
                    try {
                        BufferPagePool pagePool;
                        if (client != null) {
                            Field bufferPoolField = AioQuickClient.class.getDeclaredField("bufferPool");
                            bufferPoolField.setAccessible(true);
                            pagePool = (BufferPagePool) bufferPoolField.get(client);
                        } else {
                            Field bufferPoolField = AioQuickServer.class.getDeclaredField("bufferPool");
                            bufferPoolField.setAccessible(true);
                            pagePool = (BufferPagePool) bufferPoolField.get(server);
                        }
                        if (pagePool == null) {
                            LOGGER.error("{} maybe has not started!", client == null ? "server" : "client");
                            return;
                        }
                        Field field = BufferPagePool.class.getDeclaredField("bufferPageList");
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
            }
        }, mills, mills);
    }
}
