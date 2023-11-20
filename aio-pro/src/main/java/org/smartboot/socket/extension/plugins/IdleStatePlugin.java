package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.channels.AsynchronousSocketChannelProxy;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * 空闲IO状态监听插件
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/10/22
 */
public final class IdleStatePlugin<T> extends AbstractPlugin<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdleStatePlugin.class);

    private static final HashedWheelTimer timer = new HashedWheelTimer(r -> {
        Thread thread = new Thread(r, "idleStateMonitor");
        thread.setDaemon(true);
        return thread;
    });


    private final int idleTimeout;

    private final boolean writeMonitor;
    private final boolean readMonitor;

    public IdleStatePlugin(int idleTimeout) {
        this(idleTimeout, true, true);
    }

    public IdleStatePlugin(int idleTimeout, boolean readMonitor, boolean writeMonitor) {
        if (idleTimeout <= 0) {
            throw new IllegalArgumentException("invalid idleTimeout");
        }
        if (!writeMonitor && !readMonitor) {
            throw new IllegalArgumentException("readIdle and writeIdle both disable");
        }
        this.idleTimeout = idleTimeout;
        this.writeMonitor = writeMonitor;
        this.readMonitor = readMonitor;
    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new IdleMonitorChannel(channel);
    }

    class IdleMonitorChannel extends AsynchronousSocketChannelProxy {
        TimerTask task;
        long readTimestamp;
        long writeTimestamp;

        public IdleMonitorChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
            super(asynchronousSocketChannel);
            if (!IdleStatePlugin.this.readMonitor) {
                readTimestamp = Long.MAX_VALUE;
            }
            if (!IdleStatePlugin.this.writeMonitor) {
                writeTimestamp = Long.MAX_VALUE;
            }
            this.task = timer.scheduleWithFixedDelay(() -> {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - readTimestamp) > IdleStatePlugin.this.idleTimeout || (currentTime - writeTimestamp) > IdleStatePlugin.this.idleTimeout) {
                    try {
                        if (asynchronousSocketChannel.isOpen() && LOGGER.isDebugEnabled()) {
                            LOGGER.debug("close session:{} by IdleStatePlugin", asynchronousSocketChannel.getRemoteAddress());
                        }
                        close();
                    } catch (IOException e) {
                        LOGGER.debug("close exception", e);
                    }
                }
            }, IdleStatePlugin.this.idleTimeout, TimeUnit.MILLISECONDS);
        }

        @Override
        public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (IdleStatePlugin.this.readMonitor) {
                readTimestamp = System.currentTimeMillis();
            }
            super.read(dst, timeout, unit, attachment, handler);
        }

        @Override
        public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (IdleStatePlugin.this.writeMonitor) {
                writeTimestamp = System.currentTimeMillis();
            }
            super.write(src, timeout, unit, attachment, handler);
        }

        @Override
        public void close() throws IOException {
            task.cancel();
            super.close();
        }
    }
}
