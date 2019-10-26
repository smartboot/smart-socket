package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.util.QuickTimerTask;

import java.lang.reflect.Field;
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

    private AioQuickServer<T> server;

    public BufferPageMonitorPlugin(AioQuickServer<T> server, int seconds) {
        this.seconds = seconds;
        this.server = server;
        init();
    }

    private void init() {
        long mills = TimeUnit.SECONDS.toMillis(seconds);
        QuickTimerTask.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                {
                    if (server == null) {
                        LOGGER.error("unKnow server or client need to monitor!");
                        return;
                    }
                    try {
                        Field bufferPoolField = AioQuickServer.class.getDeclaredField("bufferPool");
                        bufferPoolField.setAccessible(true);
                        BufferPagePool pagePool = (BufferPagePool) bufferPoolField.get(server);
                        if (pagePool == null) {
                            LOGGER.error("server maybe has not started!");
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
