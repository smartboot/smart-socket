package org.smartboot.socket.transport;

import org.smartboot.socket.protocol.ProtocolFactory;
import org.smartboot.socket.service.filter.SmartFilter;
import org.smartboot.socket.service.process.ProtocolDataProcessor;

/**
 * Quickly服务端/客户端配置信息 T:解码后生成的对象类型，S:
 *
 * @author Seer
 */
final class IoServerConfig<T> {

    /**
     * 自动修复链接
     */
    private boolean autoRecover = false;

    /**
     * 消息队列缓存大小
     */
    private int cacheSize = 1024;

    /**
     * 消息体缓存大小,字节
     */
    private int dataBufferSize = 1024;

    /**
     * 远程服务器IP
     */
    private String host;

    /**
     * 本地IP
     */
    private String localIp;

    /**
     * 服务器消息拦截器
     */
    private SmartFilter<T>[] filters;

    /**
     * 服务器端口号
     */
    private int port = 8888;

    /**
     * 消息处理器
     */
    private ProtocolDataProcessor<T> processor;

    /**
     * 协议工厂
     */
    private ProtocolFactory<T> protocolFactory;


    /**
     * 服务器处理线程数
     */
    private int threadNum = Runtime.getRuntime().availableProcessors();

    /**
     * 超时时间
     */
    private int timeout = Integer.MAX_VALUE;

    /**
     * true:服务器,false:客户端
     */
    private boolean serverOrClient;

    /**
     * @param serverOrClient true:服务器,false:客户端
     */
    public IoServerConfig(boolean serverOrClient) {
        this.serverOrClient = serverOrClient;
    }

    public final int getCacheSize() {
        return cacheSize;
    }

    public final String getHost() {
        return host;
    }

    public final String getLocalIp() {
        return localIp;
    }

    public final int getPort() {
        return port;
    }


    public final int getThreadNum() {
        return threadNum;
    }

    public final int getTimeout() {
        return timeout;
    }


    public final boolean isAutoRecover() {
        return autoRecover;
    }

    public final void setAutoRecover(boolean autoRecover) {
        this.autoRecover = autoRecover;
    }

    public final void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public final void setHost(String host) {
        this.host = host;
    }

    public final void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public final void setPort(int port) {
        this.port = port;
    }


    public final void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public final void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    public final boolean isServer() {
        return serverOrClient;
    }

    public final boolean isClient() {
        return !serverOrClient;
    }

    public final int getDataBufferSize() {
        return dataBufferSize;
    }

    public final void setDataBufferSize(int dataBufferSize) {
        this.dataBufferSize = dataBufferSize;
    }

    public final SmartFilter<T>[] getFilters() {
        return filters;
    }

    public final void setFilters(SmartFilter<T>[] filters) {
        this.filters = filters;
    }

    public final ProtocolFactory<T> getProtocolFactory() {
        return protocolFactory;
    }

    public final void setProtocolFactory(ProtocolFactory<T> protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public final ProtocolDataProcessor<T> getProcessor() {
        return processor;
    }

    public final void setProcessor(ProtocolDataProcessor<T> processor) {
        this.processor = processor;
    }
}