package net.vinote.smart.socket.transport.nio;

import net.vinote.smart.socket.protocol.ProtocolFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;

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
    private int cacheSize = 256;

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
     * 读管道单论循环操作次数
     */
    private int readLoopTimes = 5;


    /**
     * 服务器处理线程数
     */
    private int threadNum = Runtime.getRuntime().availableProcessors();

    /**
     * 超时时间
     */
    private int timeout = Integer.MAX_VALUE;

    /**
     * 写管道单论循环操作次数
     */
    private int writeLoopTimes = 10;

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

    public final int getReadLoopTimes() {
        return readLoopTimes;
    }


    public final int getThreadNum() {
        return threadNum;
    }

    public final int getTimeout() {
        return timeout;
    }

    public final int getWriteLoopTimes() {
        return writeLoopTimes;
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

    public final void setReadLoopTimes(int readLoopTimes) {
        this.readLoopTimes = readLoopTimes;
    }


    public final void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public final void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public final void setWriteLoopTimes(int writeLoopTimes) {
        this.writeLoopTimes = writeLoopTimes;
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

    /**
     * Getter method for property <tt>filters</tt>.
     *
     * @return property value of filters
     */
    public final SmartFilter<T>[] getFilters() {
        return filters;
    }

    /**
     * Setter method for property <tt>filters</tt>.
     *
     * @param filters value to be assigned to property filters
     */
    public final void setFilters(SmartFilter<T>[] filters) {
        this.filters = filters;
    }

    /**
     * Getter method for property <tt>protocolFactory</tt>.
     *
     * @return property value of protocolFactory
     */
    public final ProtocolFactory<T> getProtocolFactory() {
        return protocolFactory;
    }

    /**
     * Setter method for property <tt>protocolFactory</tt>.
     *
     * @param protocolFactory value to be assigned to property protocolFactory
     */
    public final void setProtocolFactory(ProtocolFactory<T> protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    /**
     * Getter method for property <tt>processor</tt>.
     *
     * @return property value of processor
     */
    public final ProtocolDataProcessor<T> getProcessor() {
        return processor;
    }

    /**
     * Setter method for property <tt>processor</tt>.
     *
     * @param processor value to be assigned to property processor
     */
    public final void setProcessor(ProtocolDataProcessor<T> processor) {
        this.processor = processor;
    }
}
