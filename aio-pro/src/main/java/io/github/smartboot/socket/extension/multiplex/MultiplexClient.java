/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MultiplexClient.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.extension.multiplex;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.Plugin;
import io.github.smartboot.socket.Protocol;
import io.github.smartboot.socket.extension.plugins.IdleStatePlugin;
import io.github.smartboot.socket.extension.plugins.SslPlugin;
import io.github.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
    private volatile boolean closed;

    /**
     * 首次连接标识
     * <p>
     * true表示尚未建立过连接，用于初始化插件等操作
     * </p>
     */
    private boolean firstConnected = true;

    private volatile int version;

    /**
     * 可链路复用的连接队列
     * <p>
     * 存储可以复用的空闲连接，采用先进先出策略
     * </p>
     */
    private final ConcurrentLinkedDeque<AioQuickClient> reusingClients = new ConcurrentLinkedDeque<>();

    /**
     * 所有活跃连接映射表
     * <p>
     * 存储所有当前活跃的连接，用于连接管理和资源释放
     * </p>
     */
    private final ConcurrentHashMap<AioQuickClient, AioQuickClient> clients = new ConcurrentHashMap<>();

    /**
     * 信号量，仅用于限制最大连接数
     *
     */
    private volatile Semaphore semaphore;

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
        version++;
        long acquireTime = System.currentTimeMillis();
        // 更新最后使用时间
        if (semaphore == null) {
            synchronized (this) {
                if (semaphore == null) {
                    semaphore = new Semaphore(multiplexOptions.getMaxConnections());
                }
            }
        }
        if (multiplexOptions.getConnectTimeout() <= 0) {
            semaphore.acquire();
        } else if (!semaphore.tryAcquire(multiplexOptions.getConnectTimeout(), TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("Failed to acquire connection within " + multiplexOptions.getConnectTimeout() + " ms. All connections are in use and max connections limit (" + multiplexOptions.getMaxConnections() + ") has been reached.");
        }
        try {
            while (!closed) {
                //循环申请超时
                if (multiplexOptions.getConnectTimeout() > 0 && System.currentTimeMillis() - acquireTime > multiplexOptions.getConnectTimeout()) {
                    throw new IllegalStateException("Failed to acquire connection within " + multiplexOptions.getConnectTimeout() + " ms. All connections are in use and max connections limit (" + multiplexOptions.getMaxConnections() + ") has been reached.");
                }
                // 1. 优先复用连接
                AioQuickClient client;

                while ((client = reusingClients.pollFirst()) != null) {
                    AioSession session = client.getSession();

                    if (session == null || session.isInvalid()) {
                        client.shutdownNow(); // 关闭无效连接
                        clients.remove(client);
                        continue;
                    }

                    // 复用连接,回调异常不影响主流程
                    try {
                        onReuse(client);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return client;
                }

                createNewClient();
            }
        } catch (Throwable t) {
            semaphore.release();
            throw t;
        } finally {
            if (closed) {
                close();
            }
        }
        throw new IllegalStateException("client closed");
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
                        if (responsePlugin instanceof SslPlugin) {
                            noneSslPlugin = false;
                            break;
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
                    // 添加配置的插件
                    for (Plugin<T> responsePlugin : multiplexOptions.getPlugins()) {
                        processor.addPlugin(responsePlugin);
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
        reusingClients.offerLast(client);

        // 触发新连接创建回调,回调异常不影响主流程
        try {
            onNew(client);
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
     * 回收连接用于后续复用
     * <p>
     * <b>重要提示：</b>用户在使用完连接后，必须主动调用 {@link #reuse(AioQuickClient)} 或 {@link #release(AioQuickClient)} 进行资源回收。
     * </p>
     * <p>
     * 该方法将使用完毕的连接放回可复用连接队列的头部，以便后续请求可以优先复用该连接。
     * 同时释放信号量许可，允许新的连接请求被处理。
     * </p>
     * <p>
     * <b>连接池管理策略：</b>
     * <ul>
     *   <li>当信号量可用许可数达到最大连接数且活跃连接数超过最小连接数时，会自动清理多余的空闲连接</li>
     *   <li>清理策略：从复用队列尾部移除连接并关闭，保持连接池在合理规模</li>
     *   <li>此机制可以有效控制内存占用，避免空闲连接过多造成资源浪费</li>
     * </ul>
     * </p>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>用户必须确保每个获取的连接最终都会被回收（通过 reuse 或 release）</li>
     *   <li>未调用回收方法将导致连接泄漏和资源浪费</li>
     *   <li>重复调用回收方法可能导致不可预期的行为，系统不做保障</li>
     *   <li>在调用 {@link #close()} 关闭客户端后，信号量状态可能不准确，但由于服务已停止，此情况可忽略</li>
     * </ul>
     * </p>
     *
     * @param client 需要回收的客户端连接，该连接将被放入复用队列供后续请求使用
     */
    public final void reuse(AioQuickClient client) {
        int v = version;
        //提升活跃连接利用率
        reusingClients.addFirst(client);
        semaphore.release();


        while (v == version && clients.size() > multiplexOptions.getMinConnections()) {
            AioQuickClient c = reusingClients.pollLast();
            if (c == null) {
                break;
            }
            clients.remove(c);
            c.shutdownNow();
        }
    }

    /**
     * 释放并关闭连接，从连接管理器中移除
     * <p>
     * <b>重要提示：</b>用户在使用完连接后，必须主动调用 {@link #reuse(AioQuickClient)} 或 {@link #release(AioQuickClient)} 进行资源回收。
     * </p>
     * <p>
     * 该方法会立即关闭连接并从连接管理器中移除该连接，同时释放相应的资源。
     * 与 {@link #reuse(AioQuickClient)} 不同，此方法会彻底销毁连接，不再用于复用。
     * </p>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>用户必须确保每个获取的连接最终都会被回收（通过 reuse 或 release）</li>
     *   <li>未调用回收方法将导致连接泄漏和资源浪费</li>
     *   <li>重复调用回收方法可能导致不可预期的行为，系统不做保障</li>
     *   <li>在调用 {@link #close()} 关闭客户端后，信号量状态可能不准确，但由于服务已停止，此情况可忽略</li>
     * </ul>
     * </p>
     *
     * @param client 需要释放的客户端连接
     * @throws IllegalArgumentException 如果连接不属于当前多路复用客户端
     */
    public final void release(AioQuickClient client) {
        client.shutdownNow();
        // 检查连接是否属于当前多路复用客户端
        if (clients.remove(client) == null) {
            throw new IllegalArgumentException("client is not belong to this multiplex client");
        }
        semaphore.release();
    }

    /**
     * 关闭多路复用客户端
     * <p>
     * 该方法会关闭所有活跃的连接，并设置关闭状态标识。
     * </p>
     * <p>
     * <b>注意：</b>调用此方法后，信号量（semaphore）的状态可能不准确，
     * 但由于客户端已停止服务，此情况不会影响系统正常运行，可安全忽略。
     * </p>
     */
    public void close() {
        closed = true;
        clients.forEach((client, aioQuickClient) -> release(client));
    }
}