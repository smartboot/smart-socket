/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IoServerConfig.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.Protocol;

import java.net.SocketOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Quickly服务端/客户端配置信息 T:解码后生成的对象类型
 *
 * @author 三刀
 * @version V1.0.0
 */
final class IoServerConfig<T> {

    /**
     * banner信息
     */
    public static final String BANNER = "\n" +
            "                               _                           _             _   \n" +
            "                              ( )_                        ( )           ( )_ \n" +
            "  ___   ___ ___     _ _  _ __ | ,_)     ___    _      ___ | |/')    __  | ,_)\n" +
            "/',__)/' _ ` _ `\\ /'_` )( '__)| |     /',__) /'_`\\  /'___)| , <   /'__`\\| |  \n" +
            "\\__, \\| ( ) ( ) |( (_| || |   | |_    \\__, \\( (_) )( (___ | |\\`\\ (  ___/| |_ \n" +
            "(____/(_) (_) (_)`\\__,_)(_)   `\\__)   (____/`\\___/'`\\____)(_) (_)`\\____)`\\__)";

    public static final String VERSION = "v1.4.6.rc-1";

    /**
     * 消息体缓存大小,字节
     */
    private int readBufferSize = 512;

    /**
     * Write缓存区容量
     */
    private int writeQueueCapacity = 512;
    /**
     * 远程服务器IP
     */
    private String host;
    /**
     * 服务器消息拦截器
     */
    private NetMonitor<T> monitor;
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
    /**
     * 是否启用控制台banner
     */
    private boolean bannerEnabled = true;

    /**
     * Socket 配置
     */
    private Map<SocketOption<Object>, Object> socketOptions;

    /**
     * 线程数
     */
    private int threadNum = 1;

    /**
     * 内存页大小
     */
    private int bufferPoolPageSize;

    /**
     * 内存页个数
     */
    private int bufferPoolPageNum;

    /**
     * 内存块大小限制
     */
    private int bufferPoolChunkSize = 4096;

    /**
     * 是否使用直接缓冲区内存
     */
    private boolean bufferPoolDirect = true;

    public int getBufferPoolPageSize() {
        return bufferPoolPageSize;
    }

    public void setBufferPoolPageSize(int bufferPoolPageSize) {
        this.bufferPoolPageSize = bufferPoolPageSize;
    }

    public int getBufferPoolPageNum() {
        return bufferPoolPageNum;
    }

    public void setBufferPoolPageNum(int bufferPoolPageNum) {
        this.bufferPoolPageNum = bufferPoolPageNum;
    }

    public int getBufferPoolChunkSize() {
        return bufferPoolChunkSize;
    }

    public void setBufferPoolChunkSize(int bufferPoolChunkSize) {
        this.bufferPoolChunkSize = bufferPoolChunkSize;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public NetMonitor<T> getMonitor() {
        return monitor;
    }

    public Protocol<T> getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol<T> protocol) {
        this.protocol = protocol;
    }

    public MessageProcessor<T> getProcessor() {
        return processor;
    }

    public void setProcessor(MessageProcessor<T> processor) {
        this.processor = processor;
        this.monitor = (processor instanceof NetMonitor) ? (NetMonitor<T>) processor : null;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    public boolean isBannerEnabled() {
        return bannerEnabled;
    }

    public void setBannerEnabled(boolean bannerEnabled) {
        this.bannerEnabled = bannerEnabled;
    }

    public Map<SocketOption<Object>, Object> getSocketOptions() {
        return socketOptions;
    }

    public void setOption(SocketOption socketOption, Object f) {
        if (socketOptions == null) {
            socketOptions = new HashMap<>(4);
        }
        socketOptions.put(socketOption, f);
    }

    public int getWriteQueueCapacity() {
        return writeQueueCapacity;
    }

    public void setWriteQueueCapacity(int writeQueueCapacity) {
        this.writeQueueCapacity = writeQueueCapacity;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public boolean isBufferPoolDirect() {
        return bufferPoolDirect;
    }

    public void setBufferPoolDirect(boolean bufferPoolDirect) {
        this.bufferPoolDirect = bufferPoolDirect;
    }

    @Override
    public String toString() {
        return "IoServerConfig{" +
                ", readBufferSize=" + readBufferSize +
                ", host='" + host + '\'' +
                ", monitor=" + monitor +
                ", port=" + port +
                ", processor=" + processor +
                ", protocol=" + protocol +
                ", bannerEnabled=" + bannerEnabled +
                ", socketOptions=" + socketOptions +
                '}';
    }
}
