package org.smartboot.socket.extension.multiplex;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.plugins.IdleStatePlugin;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多路复用客户端实现类
 * <p>
 * 该类实现了连接的复用机制，可以有效减少频繁创建和销毁连接的开销。
 * 通过连接池管理和复用策略，提高系统性能和资源利用率。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>连接池管理：维护可用连接队列和所有活跃连接映射</li>
 *   <li>连接复用：优先从连接池中获取可用连接，避免重复创建</li>
 *   <li>连接监控：定期清理空闲连接，保持连接池健康</li>
 *   <li>资源配置：支持SSL、插件、缓冲区等参数配置</li>
 * </ul>
 * </p>
 *
 * @param <T> 泛型参数，表示传输的消息类型
 * @author 三刀
 */
public class MultiplexClient<T> {
    /**
     * 多路复用客户端配置选项
     * <p>
     * 包含客户端的各种配置参数，如连接参数、缓冲区配置、超时设置等
     * </p>
     */
    protected final MultiplexOptions<T> multiplexOptions = new MultiplexOptions<>();

    /**
     * 消息处理器
     * <p>
     * 用于处理接收到的消息，实现业务逻辑
     * </p>
     */
    private final AbstractMessageProcessor<T> processor;

    /**
     * 协议编解码器
     * <p>
     * 用于对传输的数据进行编码和解码操作
     * </p>
     */
    private final Protocol<T> protocol;

    /**
     * 客户端关闭状态标识
     * <p>
     * true表示客户端已关闭，不再接受新的连接请求
     * </p>
     */
    private boolean closed;

    /**
     * 首次连接标识
     * <p>
     * true表示尚未建立过连接，用于初始化插件等操作
     * </p>
     */
    private boolean firstConnected = true;

    /**
     * 可链路复用的连接队列
     * <p>
     * 存储可以复用的空闲连接，采用先进先出策略
     * </p>
     */
    private final ConcurrentLinkedQueue<AioQuickClient> resuingClients = new ConcurrentLinkedQueue<>();

    /**
     * 所有活跃连接映射表
     * <p>
     * 存储所有当前活跃的连接，用于连接管理和资源释放
     * </p>
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
     * 当创建新的连接请求时会更新此时间戳，监控任务会定期检查此值
     * 以判断连接是否已空闲超过指定时间（默认60秒）
     * </p>
     */
    private long latestTime = System.currentTimeMillis();

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    /**
     * 构造函数
     *
     * @param processor 消息处理器，用于处理接收到的消息
     * @param protocol  协议编解码器，用于对传输的数据进行编码和解码
     */
    public MultiplexClient(AbstractMessageProcessor<T> processor, Protocol<T> protocol) {
        this.processor = processor;
        this.protocol = protocol;
    }

    /**
     * 获取多路复用客户端配置选项
     *
     * @return 多路复用客户端配置选项实例
     */
    public MultiplexOptions<T> getMultiplexOptions() {
        return multiplexOptions;
    }

    /**
     * 获取可用的客户端连接
     * <p>
     * 该方法首先尝试从可复用连接队列中获取空闲连接，
     * 如果没有可用的空闲连接，则创建新的连接。
     * </p>
     *
     * @return 可用的AioQuickClient实例
     * @throws Throwable 如果获取连接过程中发生异常
     */
    public final AioQuickClient acquire() throws Throwable {
        // 检查客户端是否已关闭
        if (closed) {
            throw new IllegalStateException("client closed");
        }

        // 更新最后使用时间
        latestTime = System.currentTimeMillis();

        AioQuickClient client;
        // 循环尝试从可复用连接队列中获取连接
        while (true) {
            client = resuingClients.poll();
            if (client == null) {
                break;
            }

            AioSession session = client.getSession();
            // 检查连接是否有效
            if (session == null || session.isInvalid()) {
                release(client);
                continue;
            }

            // 触发连接复用回调
            onReuse(client);
            return client;
        }

        // 创建新的连接
        boolean wait = true;
        if (clients.size() < multiplexOptions.getMaxConnections()) {
            synchronized (this) {
                if (clients.size() < multiplexOptions.getMaxConnections()) {
                    wait = false;
                    createNewClient();
                }
            }
        }
        if (wait && resuingClients.isEmpty()) {
            lock.lock();
            try {
                condition.await();
            } finally {
                lock.unlock();
            }
        }
        // 递归调用，直到获取到有效的连接
        return acquire();
    }

    /**
     * 创建新的客户端连接
     * <p>
     * 该方法负责创建新的连接，并进行初始化配置，
     * 包括插件配置、缓冲区设置等。
     * </p>
     *
     * @throws Exception 如果创建连接过程中发生异常
     */
    private void createNewClient() throws Exception {
        // 首次连接时进行插件初始化
        if (firstConnected) {
            synchronized (this) {
                if (firstConnected) {
                    boolean noneSslPlugin = true;
                    // 添加配置的插件
                    for (Plugin<T> responsePlugin : multiplexOptions.getPlugins()) {
                        processor.addPlugin(responsePlugin);
                        if (responsePlugin instanceof SslPlugin) {
                            noneSslPlugin = false;
                        }
                    }

                    // 如果启用了SSL但没有配置SSL插件，则自动添加默认SSL插件
                    if (noneSslPlugin && multiplexOptions.isSsl()) {
                        processor.addPlugin(new SslPlugin<>(new ClientSSLContextFactory()));
                    }

                    // 如果配置了空闲超时时间，则添加空闲状态插件
                    if (multiplexOptions.idleTimeout() > 0) {
                        processor.addPlugin(new IdleStatePlugin<>(multiplexOptions.idleTimeout()));
                    }

                    firstConnected = false;
                }
            }
        }

        // 创建AioQuickClient实例
        AioQuickClient client = new AioQuickClient(multiplexOptions.getHost(), multiplexOptions.getPort(), protocol, processor);

        // 配置缓冲区大小
        client.setReadBufferSize(multiplexOptions.getReadBufferSize()).setWriteBuffer(multiplexOptions.getWriteChunkSize(), multiplexOptions.getWriteChunkCount());

        // 配置内存池
        client.setBufferPagePool(multiplexOptions.getReadBufferPool(), multiplexOptions.getWriteBufferPool());


        // 配置连接超时时间
        if (multiplexOptions.getConnectTimeout() > 0) {
            client.connectTimeout(multiplexOptions.getConnectTimeout());
        }

        // 启动客户端连接
        if (multiplexOptions.group() == null) {
            client.start();
        } else {
            client.start(multiplexOptions.group());
        }

        // 将新创建的连接添加到连接管理器中
        clients.put(client, client);
        resuingClients.offer(client);

        // 启动连接监控任务
        startConnectionMonitor();

        // 触发新连接创建回调
        onNew(client);
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
     * </p>
     *
     * @param client 新创建的AIO会话实例
     */
    protected void onNew(AioQuickClient client) {
        // 子类可以重写此方法来处理新建立的连接
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
     * </p>
     *
     * @param client 被复用的AIO会话实例
     */
    protected void onReuse(AioQuickClient client) {
        // 子类可以重写此方法来处理连接复用场景
    }

    /**
     * 启动连接监控任务，用于清理无效连接和空闲连接
     *
     * <p>该方法会启动一个定时任务，每隔1分钟执行一次检查：
     * <ul>
     *   <li>如果连接超过60秒没有被使用，则将其关闭</li>
     *   <li>如果所有连接都已关闭，则取消监控任务</li>
     *   <li>如果任务取消后又有新连接创建，则重新启动监控任务</li>
     * </ul>
     * </p>
     *
     * @see #release(AioQuickClient)
     * @see #acquire()
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

            // 启动定时任务，每隔1分钟执行一次连接检查
            timerTask = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                long time = latestTime;
                // 如果超过60秒没有使用连接，则清理可复用连接队列中的连接
                if (System.currentTimeMillis() - time > 60 * 1000) {
                    AioQuickClient c;
                    while (time == latestTime && clients.size() > multiplexOptions.getMinConnections() && (c = resuingClients.poll()) != null) {
//                        System.out.println("release...");
                        release(c);
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

                // 确保维持最小连接数
                maintainMinConnections();
            }, 1, TimeUnit.MINUTES);
            // 确保维持最小连接数
            maintainMinConnections();
        }
    }

    /**
     * 维持最小连接数
     *
     * <p>该方法确保连接池中至少维持指定数量的连接，
     * 即使在空闲状态下也不会低于该数量。</p>
     */
    private void maintainMinConnections() {

        // 如果当前连接数小于最小连接数，则创建新的连接
        while (clients.size() < multiplexOptions.getMinConnections()) {
            try {
                createNewClient();
            } catch (Exception e) {
                // 创建连接失败，记录日志但不中断操作
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * 回收连接用于后续复用
     * <p>
     * 将使用完毕的连接放回可复用连接队列中，以便后续请求可以复用该连接。
     * </p>
     *
     * @param client 需要回收的客户端连接
     * @throws IllegalArgumentException 如果连接不属于当前多路复用客户端
     */
    public final void reuse(AioQuickClient client) {
        // 检查连接是否属于当前多路复用客户端
        if (!clients.containsKey(client)) {
            throw new IllegalArgumentException("client is not belong to this multiplex client");
        }

        // 将连接放回可复用连接队列
        resuingClients.offer(client);

        // 释放信号量
        releaseSemaphore();
    }

    /**
     * 释放并关闭连接，从连接管理器中移除
     * <p>
     * 该方法会立即关闭连接并从连接管理器中移除该连接，
     * 同时释放相应的资源。
     * </p>
     *
     * @param client 需要释放的客户端连接
     * @throws IllegalArgumentException 如果连接不属于当前多路复用客户端
     */
    public final void release(AioQuickClient client) {
        // 检查连接是否属于当前多路复用客户端
        if (!clients.containsKey(client)) {
            throw new IllegalArgumentException("client is not belong to this multiplex client");
        }

        try {
            // 立即关闭连接
            client.shutdownNow();

            // 从连接管理器中移除连接
            clients.remove(client);
        } finally {
            // 释放信号量，允许创建新连接
            releaseSemaphore();
        }
    }

    /**
     * 释放信号量许可
     * <p>
     * 释放一个信号量许可，允许创建新的连接。
     * </p>
     */
    private void releaseSemaphore() {
        lock.lock();
        try {
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 关闭多路复用客户端
     * <p>
     * 该方法会关闭所有活跃的连接，并设置关闭状态标识。
     * </p>
     */
    public void close() {
        closed = true;
        clients.forEach((client, aioQuickClient) -> release(client));
    }

}