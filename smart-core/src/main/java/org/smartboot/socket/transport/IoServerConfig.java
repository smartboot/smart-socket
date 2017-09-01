package org.smartboot.socket.transport;

import org.smartboot.socket.protocol.Protocol;
import org.smartboot.socket.service.filter.SmartFilter;
import org.smartboot.socket.service.process.MessageProcessor;

/**
 * Quickly服务端/客户端配置信息 T:解码后生成的对象类型，S:
 *
 * @author Seer
 */
final class IoServerConfig<T> {

    /**
     * 消息队列缓存大小
     */
    private int writeQueueSize = 512;

    /**
     * 消息体缓存大小,字节
     */
    private int readBufferSize = 512;

    /**
     * 远程服务器IP
     */
    private String host;


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
    private MessageProcessor<T> processor;

    /**
     * 协议编解码
     */
    private Protocol<T> protocol;


    private static final int CPU_NUM=Runtime.getRuntime().availableProcessors();
    /**
     * 服务器处理线程数
     */
    private int threadNum = CPU_NUM;

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

    public final String getHost() {
        return host;
    }


    public final int getPort() {
        return port;
    }


    public final int getThreadNum() {
        return threadNum;
    }


    public final void setHost(String host) {
        this.host = host;
    }


    public final void setPort(int port) {
        this.port = port;
    }


    public final void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }


    public final boolean isServer() {
        return serverOrClient;
    }

    public final boolean isClient() {
        return !serverOrClient;
    }


    public final SmartFilter<T>[] getFilters() {
        return filters;
    }

    public final void setFilters(SmartFilter<T>[] filters) {
        this.filters = filters;
    }

    public Protocol<T> getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol<T> protocol) {
        this.protocol = protocol;
    }

    public final MessageProcessor<T> getProcessor() {
        return processor;
    }

    public final void setProcessor(MessageProcessor<T> processor) {
        this.processor = processor;
    }

    public int getWriteQueueSize() {
        return writeQueueSize;
    }

    public void setWriteQueueSize(int writeQueueSize) {
        this.writeQueueSize = writeQueueSize;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }
}
