package org.smartboot.socket.extension.multiplex;

import org.smartboot.socket.extension.plugins.Plugin;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * 多路复用客户端配置选项类
 * 
 * 该类用于配置多路复用客户端的各种参数，包括连接参数、缓冲区配置、
 * 超时设置、SSL配置等。通过该类可以灵活地控制客户端的行为。
 * 
 * @param <T> 泛型参数，表示传输的消息类型
 */
public class MultiplexOptions<T> {

    /**
     * 绑定线程池资源组
     * 
     * 用于指定客户端使用的异步通道组，如果为null则使用默认的通道组
     */
    private AsynchronousChannelGroup group;
    /**
     * 连接超时时间（毫秒）
     * 
     * 客户端尝试建立连接时的超时时间，0表示不设置超时
     */
    private int connectTimeout;
    /**
     * 空闲超时时间，单位：毫秒
     * 
     * 连接在无操作状态下的最大存活时间，超过该时间的空闲连接将被关闭
     * 默认值为60000毫秒（1分钟）
     */
    private int idleTimeout = 60000;

    /**
     * read缓冲区大小（字节）
     * 
     * 用于设置读取数据时使用的缓冲区大小，影响读取性能和内存占用
     * 默认值为1024字节
     */
    private int readBufferSize = 1024;

    /**
     * write缓冲区块大小（字节）
     * 
     * 用于设置写入数据时单个缓冲区块的大小，影响写入性能
     * 默认值为1024字节
     */
    private int writeChunkSize = 1024;

    /**
     * write缓冲区块个数
     * 
     * 用于设置写入缓冲区中缓冲块的数量，影响可缓存的写入数据量
     * 默认值为16个块
     */
    private int writeChunkCount = 16;

    /**
     * 目标服务器主机地址
     * 
     * 客户端需要连接的服务器IP地址或主机名
     */
    private String host;
    /**
     * 目标服务器端口号
     * 
     * 客户端需要连接的服务器端口号
     */
    private int port;
    /**
     * SSL加密开关
     * 
     * 是否启用SSL/TLS加密传输，true表示启用，false表示不启用
     */
    private boolean ssl;
    /**
     * 最大连接数限制
     * 
     * 控制客户端可以同时维持的最大连接数量，防止资源耗尽
     * 默认值为128个连接
     */
    private int maxConnections = 128;
    /**
     * smart-socket 插件列表
     * 
     * 用于存储客户端使用的各种插件，如SSL插件、空闲检测插件等
     * 插件会在连接的不同阶段被调用，实现额外的功能
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
}