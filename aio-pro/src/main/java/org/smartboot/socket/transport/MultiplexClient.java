package org.smartboot.socket.transport;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.plugins.IdleStatePlugin;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version v1.0 10/22/25
 */
public abstract class MultiplexClient<T> {
    protected final Options multiplexOptions = new Options();
    private boolean closed;
    private boolean firstConnected = true;
    /**
     * 可链路复用的连接
     */
    private final ConcurrentLinkedQueue<AioQuickClient> resuingClients = new ConcurrentLinkedQueue<>();
    /**
     * 所有连接
     */
    private final ConcurrentHashMap<AioQuickClient, AioQuickClient> clients = new ConcurrentHashMap<>();
    /**
     * 连接监控定时任务
     * <p>
     * 用于定期清理空闲连接的定时任务引用，通过双重检查锁定确保
     * 系统中只有一个监控任务在运行，避免资源浪费
     * </p>
     */
    private volatile TimerTask timerTask;
    /**
     * 记录最后一次使用连接的时间戳，用于连接空闲超时检测
     * <p>
     * 当创建新的HTTP请求时会更新此时间戳，监控任务会定期检查此值
     * 以判断连接是否已空闲超过指定时间（默认30秒）
     * </p>
     */
    private long latestTime = System.currentTimeMillis();

    protected final AioQuickClient acquireClient() throws Throwable {
        if (closed) {
            throw new IllegalStateException("client closed");
        }
        latestTime = System.currentTimeMillis();
        AioQuickClient client;
        while (true) {
            client = resuingClients.poll();
            if (client == null) {
                break;
            }
            AioSession session = client.getSession();
            if (session == null || session.isInvalid()) {
                releaseClient(client);
                continue;
            }
            onReuseClient(client);
            return client;
        }

        synchronized (this) {
            if (firstConnected) {
                boolean noneSslPlugin = true;
                for (Plugin<T> responsePlugin : multiplexOptions.getPlugins()) {
                    multiplexOptions.processor.addPlugin(responsePlugin);
                    if (responsePlugin instanceof SslPlugin) {
                        noneSslPlugin = false;
                    }
                }
                if (noneSslPlugin && multiplexOptions.isSsl()) {
                    multiplexOptions.processor.addPlugin(new SslPlugin<>(new ClientSSLContextFactory()));
                }
                if (multiplexOptions.idleTimeout() > 0) {
                    multiplexOptions.processor.addPlugin(new IdleStatePlugin<>(multiplexOptions.idleTimeout()));
                }

                firstConnected = false;
            }
        }

        client = new AioQuickClient(multiplexOptions.getHost(), multiplexOptions.getPort(), multiplexOptions.protocol, multiplexOptions.processor);
        client.setReadBufferSize(multiplexOptions.readBufferSize).setWriteBuffer(multiplexOptions.writeChunkSize, multiplexOptions.writeChunkCount);
        if (multiplexOptions.getConnectTimeout() > 0) {
            client.connectTimeout(multiplexOptions.getConnectTimeout());
        }
        if (multiplexOptions.group() == null) {
            client.start();
        } else {
            client.start(multiplexOptions.group());
        }

        clients.put(client, client);
        resuingClients.offer(client);
        startConnectionMonitor();
        onNewClient(client);
        return client;
    }

    /**
     * 当创建新连接时触发此回调方法
     *
     * <p>子类可以重写此方法来处理新建立的会话，例如：
     * <ul>
     *   <li>记录连接日志</li>
     *   <li>初始化会话属性</li>
     *   <li>发送握手消息</li>
     *   <li>设置会话监听器</li>
     * </ul>
     *
     * @param client 新创建的AIO会话实例
     * @since 1.0
     */
    protected void onNewClient(AioQuickClient client) {
    }

    /**
     * 当复用已有连接时触发此回调方法
     *
     * <p>子类可以重写此方法来处理连接复用场景，例如：
     * <ul>
     *   <li>重置会话状态</li>
     *   <li>更新会话属性</li>
     *   <li>记录复用日志</li>
     *   <li>执行预处理逻辑</li>
     * </ul>
     *
     * @param client 被复用的AIO会话实例
     * @since 1.0
     */
    protected void onReuseClient(AioQuickClient client) {

    }

    /**
     * 启动连接监控任务，用于清理无效连接和空闲连接
     *
     * <p>该方法会启动一个定时任务，每隔1分钟执行一次检查：
     * <ul>
     *   <li>如果连接超过30秒没有被使用，则将其关闭</li>
     *   <li>如果所有连接都已关闭，则取消监控任务</li>
     *   <li>如果任务取消后又有新连接创建，则重新启动监控任务</li>
     * </ul>
     *
     * @see #releaseClient(AioQuickClient)
     * @see #acquireClient()
     */
    private void startConnectionMonitor() {
        // 使用双重检查锁定确保只有一个监控任务在运行
        if (timerTask != null) {
            return;
        }
        synchronized (this) {
            if (timerTask != null) {
                return;
            }
            timerTask = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                long time = latestTime;
                // 如果超过30秒没有使用连接，则清理可复用连接队列中的连接
                if (System.currentTimeMillis() - time > 30 * 1000) {
                    AioQuickClient c;
                    // 当latestTime没有更新且队列中还有连接时，持续清理
                    while (time == latestTime && (c = resuingClients.poll()) != null) {
                        System.out.println("release...");
                        releaseClient(c);
                    }
                }
                // 如果没有活动连接，则取消监控任务
                if (clients.isEmpty()) {
                    TimerTask oldTask = timerTask;
                    timerTask = null;
                    oldTask.cancel();
                    // 取消任务后再次检查是否有新连接加入，如果有则重新启动监控任务
                    if (!clients.isEmpty()) {
                        startConnectionMonitor();
                    }
                }
            }, 1, TimeUnit.MINUTES);
        }
    }

    /**
     * 回收连接用于后续复用
     *
     * @param client 需要回收的客户端连接
     */
    protected final void reuseClient(AioQuickClient client) {
        resuingClients.offer(client);
    }

    /**
     * 释放并关闭连接，从连接管理器中移除
     *
     * @param client 需要释放的客户端连接
     */
    protected final void releaseClient(AioQuickClient client) {
        client.shutdownNow();
        clients.remove(client);
    }

    public void close() {
        closed = true;
        clients.forEach((client, aioQuickClient) -> releaseClient(client));
    }

    public class Options {
        private AbstractMessageProcessor<T> processor;
        private Protocol<T> protocol;
        /**
         * 绑定线程池资源组
         */
        private AsynchronousChannelGroup group;
        /**
         * 连接超时时间
         */
        private int connectTimeout;
        /**
         * 空闲超时时间，单位：毫秒
         */
        private int idleTimeout = 60000;

        /**
         * read缓冲区大小
         */
        private int readBufferSize = 1024;

        /**
         * write缓冲区块大小
         */
        private int writeChunkSize = 1024;

        /**
         * write缓冲区块个数
         */
        private int writeChunkCount = 16;

        private String host;
        private int port;
        private boolean ssl;
        /**
         * smart-socket 插件
         */
        private final List<Plugin<T>> plugins = new ArrayList<>();

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setSsl(boolean ssl) {
            this.ssl = ssl;
        }

        String getHost() {
            return host;
        }

        int getPort() {
            return port;
        }

        boolean isSsl() {
            return ssl;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        List<Plugin<T>> getPlugins() {
            return plugins;
        }

        public Options addPlugin(Plugin<T> plugin) {
            plugins.add(plugin);
            return this;
        }

        public Options group(AsynchronousChannelGroup group) {
            this.group = group;
            return this;
        }

        public AsynchronousChannelGroup group() {
            return group;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        /**
         * 设置建立连接的超时时间
         */
        protected Options connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public int idleTimeout() {
            return idleTimeout;
        }

        public Options idleTimeout(int idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        public void setWriteBuffer(int chunkSize, int chunkCount) {
            this.writeChunkSize = chunkSize;
            this.writeChunkCount = chunkCount;
        }

        public void setReadBuffer(int readBufferSize) {
            this.readBufferSize = readBufferSize;
        }

        public void init(Protocol<T> protocol, AbstractMessageProcessor<T> processor) {
            this.protocol = protocol;
            this.processor = processor;
        }
    }
}
