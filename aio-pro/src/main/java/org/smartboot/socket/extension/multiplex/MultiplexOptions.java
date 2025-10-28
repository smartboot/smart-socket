package org.smartboot.socket.extension.multiplex;

import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.Plugin;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * 多路复用客户端配置选项类
 *
 * <p>该类用于配置多路复用客户端的各种参数，包括连接参数、缓冲区配置、
 * 超时设置、SSL配置等。通过该类可以灵活地控制客户端的行为。</p>
 *
 * <p>主要配置项包括：
 * <ul>
 *   <li>网络连接参数：主机地址、端口、连接超时等</li>
 *   <li>缓冲区配置：读写缓冲区大小和数量</li>
 *   <li>安全配置：SSL加密开关</li>
 *   <li>性能配置：最大连接数、最小连接数、空闲超时时间</li>
 *   <li>扩展配置：插件列表</li>
 * </ul>
 * </p>
 *
 * @param <T> 泛型参数，表示传输的消息类型
 */
public class MultiplexOptions<T> {

    /**
     * 绑定线程池资源组
     *
     * <p>用于指定客户端使用的异步通道组，如果为null则使用默认的通道组。
     * 通过共享AsynchronousChannelGroup可以在多个客户端之间共享线程资源，
     * 提高资源利用率。</p>
     */
    private AsynchronousChannelGroup group;

    /**
     * 连接超时时间（毫秒）
     *
     * <p>客户端尝试建立连接时的超时时间，0表示不设置超时。
     * 在网络不稳定或服务器响应慢的情况下，合理设置该值可以避免连接阻塞。</p>
     */
    private int connectTimeout;

    /**
     * 空闲超时时间，单位：毫秒
     *
     * <p>连接在无操作状态下的最大存活时间，超过该时间的空闲连接将被关闭。
     * 此机制有助于及时释放无用连接，避免资源浪费，默认值为60000毫秒（1分钟）。</p>
     */
    private int idleTimeout = 60000;

    /**
     * read缓冲区大小（字节）
     *
     * <p>用于设置读取数据时使用的缓冲区大小，影响读取性能和内存占用。
     * 较大的缓冲区可以减少系统调用次数，但会增加内存消耗，默认值为1024字节。</p>
     */
    private int readBufferSize = 1024;

    /**
     * write缓冲区块大小（字节）
     *
     * <p>用于设置写入数据时单个缓冲区块的大小，影响写入性能。
     * 较大的块大小可以提高批量写入效率，默认值为1024字节。</p>
     */
    private int writeChunkSize = 1024;

    /**
     * write缓冲区块个数
     *
     * <p>用于设置写入缓冲区中缓冲块的数量，影响可缓存的写入数据量。
     * 增加块数量可以容纳更多待写入数据，但也增加内存占用，默认值为16个块。</p>
     */
    private int writeChunkCount = 16;

    /**
     * 目标服务器主机地址
     *
     * <p>客户端需要连接的服务器IP地址或主机名。
     * 支持IPv4、IPv6地址以及域名格式。</p>
     */
    private String host;

    /**
     * 目标服务器端口号
     *
     * <p>客户端需要连接的服务器端口号。
     * 有效范围是1-65535，其中1-1023为系统保留端口。</p>
     */
    private int port;

    /**
     * SSL加密开关
     *
     * <p>是否启用SSL/TLS加密传输，true表示启用，false表示不启用。
     * 启用SSL可以保证数据传输的安全性，但会增加计算开销。</p>
     */
    private boolean ssl;

    /**
     * 最大连接数限制
     *
     * <p>控制客户端可以同时维持的最大连接数量，防止资源耗尽。
     * 合理设置该值可以在系统性能和资源消耗之间取得平衡，默认值为128个连接。</p>
     */
    private int maxConnections = 128;

    /**
     * 最小连接数
     *
     * <p>控制客户端维持的最小连接数量，即使在空闲状态下也会保持该数量的连接。
     * 这样可以避免频繁创建和销毁连接的开销，默认值为0。</p>
     */
    private int minConnections = 0;

    /**
     * 读取内存池
     *
     * <p>用于设置客户端读取操作使用的内存池，通过共享内存池可以提高内存利用率。</p>
     */
    private BufferPagePool readBufferPool;

    /**
     * 写入内存池
     *
     * <p>用于设置客户端写入操作使用的内存池，通过共享内存池可以提高内存利用率。</p>
     */
    private BufferPagePool writeBufferPool;

    /**
     * smart-socket 插件列表
     *
     * <p>用于存储客户端使用的各种插件，如SSL插件、空闲检测插件等。
     * 插件会在连接的不同阶段被调用，实现额外的功能扩展。</p>
     */
    private final List<Plugin<T>> plugins = new ArrayList<>();

    /**
     * 设置目标服务器主机地址
     *
     * @param host 服务器主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 设置目标服务器端口号
     *
     * @param port 服务器端口号
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 设置是否启用SSL加密传输
     *
     * @param ssl true表示启用SSL，false表示禁用SSL
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * 获取目标服务器主机地址
     *
     * @return 服务器主机地址
     */
    String getHost() {
        return host;
    }

    /**
     * 获取目标服务器端口号
     *
     * @return 服务器端口号
     */
    int getPort() {
        return port;
    }

    /**
     * 判断是否启用了SSL加密传输
     *
     * @return true表示启用SSL，false表示未启用SSL
     */
    boolean isSsl() {
        return ssl;
    }

    /**
     * 获取插件列表
     *
     * @return 插件列表
     */
    List<Plugin<T>> getPlugins() {
        return plugins;
    }

    /**
     * 添加插件到插件列表
     *
     * @param plugin 要添加的插件实例
     * @return 返回当前配置对象，支持链式调用
     */
    public MultiplexOptions<T> addPlugin(Plugin<T> plugin) {
        plugins.add(plugin);
        return this;
    }

    /**
     * 设置异步通道组
     *
     * @param group 异步通道组实例
     * @return 返回当前配置对象，支持链式调用
     */
    public MultiplexOptions<T> group(AsynchronousChannelGroup group) {
        this.group = group;
        return this;
    }

    /**
     * 获取异步通道组
     *
     * @return 异步通道组实例
     */
    public AsynchronousChannelGroup group() {
        return group;
    }

    /**
     * 获取连接超时时间
     *
     * @return 连接超时时间（毫秒）
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置建立连接的超时时间
     *
     * @param connectTimeout 连接超时时间（毫秒）
     * @return 返回当前配置对象，支持链式调用
     */
    public MultiplexOptions<T> connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 获取空闲超时时间
     *
     * @return 空闲超时时间（毫秒）
     */
    int idleTimeout() {
        return idleTimeout;
    }

    /**
     * 设置空闲超时时间
     *
     * @param idleTimeout 空闲超时时间（毫秒）
     * @return 返回当前配置对象，支持链式调用
     */
    public MultiplexOptions<T> idleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    /**
     * 获取最大连接数限制
     *
     * @return 最大连接数
     */
    int getMaxConnections() {
        return maxConnections;
    }

    /**
     * 设置最大连接数限制
     *
     * @param maxConnections 最大连接数
     * @return 返回当前配置对象，支持链式调用
     */
    public MultiplexOptions<T> maxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    /**
     * 获取最小连接数
     *
     * @return 最小连接数
     */
    int getMinConnections() {
        return minConnections;
    }

    /**
     * 设置最小连接数
     *
     * @param minConnections 最小连接数
     * @return 返回当前配置对象，支持链式调用
     */
    public MultiplexOptions<T> minConnections(int minConnections) {
        this.minConnections = minConnections;
        return this;
    }

    /**
     * 设置写缓冲区参数
     *
     * @param chunkSize  单个缓冲区块大小（字节）
     * @param chunkCount 缓冲区块数量
     */
    public void setWriteBuffer(int chunkSize, int chunkCount) {
        this.writeChunkSize = chunkSize;
        this.writeChunkCount = chunkCount;
    }

    /**
     * 设置读缓冲区大小
     *
     * @param readBufferSize 读缓冲区大小（字节）
     */
    public void setReadBuffer(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    /**
     * 获取空闲超时时间
     *
     * @return 空闲超时时间（毫秒）
     */
    int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * 获取读缓冲区大小
     *
     * @return 读缓冲区大小（字节）
     */
    int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * 获取写缓冲区块大小
     *
     * @return 写缓冲区块大小（字节）
     */
    int getWriteChunkSize() {
        return writeChunkSize;
    }

    /**
     * 获取写缓冲区块数量
     *
     * @return 写缓冲区块数量
     */
    int getWriteChunkCount() {
        return writeChunkCount;
    }

    /**
     * 获取读取内存池
     *
     * @return 读取内存池
     */
    BufferPagePool getReadBufferPool() {
        return readBufferPool;
    }


    /**
     * 获取写入内存池
     *
     * @return 写入内存池
     */
    BufferPagePool getWriteBufferPool() {
        return writeBufferPool;
    }

    /**
     * 设置写入内存池
     *
     * @param writeBufferPool 写入内存池
     * @return 返回当前配置对象，支持链式调用
     */
    public MultiplexOptions<T> setBufferPool(BufferPagePool readBufferPool, BufferPagePool writeBufferPool) {
        this.readBufferPool = readBufferPool;
        this.writeBufferPool = writeBufferPool;
        return this;
    }
}