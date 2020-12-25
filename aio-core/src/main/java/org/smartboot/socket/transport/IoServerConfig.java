/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IoServerConfig.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.buffer.BufferFactory;

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
    /**
     * 当前smart-socket版本号
     */
    public static final String VERSION = "v1.5.5-SNAPSHOT";

    /**
     * 消息体缓存大小,字节
     */
    private int readBufferSize = 512;
    /**
     * 内存块大小限制
     */
    private int writeBufferSize = 128;
    /**
     * Write缓存区容量
     */
    private int writeBufferCapacity = 16;
    /**
     * 远程服务器IP
     */
    private String host;
    /**
     * 服务器消息拦截器
     */
    private NetMonitor monitor;
    /**
     * 服务器端口号
     */
    private int port = 8888;

    /**
     * 服务端backlog
     */
    private int backlog = 1000;

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
     * 内存池工厂
     */
    private BufferFactory bufferFactory = BufferFactory.DISABLED_BUFFER_FACTORY;


    /**
     * 获取默认内存块大小
     *
     * @return 内存块大小
     */
    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    /**
     * @param writeBufferSize 内存块大小
     */
    public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }

    /**
     * @return 主机地址
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host 主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port 端口号
     */
    public void setPort(int port) {
        this.port = port;
    }

    public NetMonitor getMonitor() {
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

    /**
     * @param processor 消息处理器
     */
    public void setProcessor(MessageProcessor<T> processor) {
        this.processor = processor;
        this.monitor = (processor instanceof NetMonitor) ? (NetMonitor) processor : null;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * @param readBufferSize 读缓冲大小
     */
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

    /**
     * @param socketOption socketOption名称
     * @param f            socketOption值
     */
    public void setOption(SocketOption socketOption, Object f) {
        if (socketOptions == null) {
            socketOptions = new HashMap<>(4);
        }
        socketOptions.put(socketOption, f);
    }

    public int getWriteBufferCapacity() {
        return writeBufferCapacity;
    }

    public void setWriteBufferCapacity(int writeBufferCapacity) {
        this.writeBufferCapacity = writeBufferCapacity;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public BufferFactory getBufferFactory() {
        return bufferFactory;
    }

    public void setBufferFactory(BufferFactory bufferFactory) {
        this.bufferFactory = bufferFactory;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    @Override
    public String toString() {
        return "IoServerConfig{" +
                "readBufferSize=" + readBufferSize +
                ", writeQueueCapacity=" + writeBufferCapacity +
                ", host='" + host + '\'' +
                ", monitor=" + monitor +
                ", port=" + port +
                ", processor=" + processor +
                ", protocol=" + protocol +
                ", bannerEnabled=" + bannerEnabled +
                ", socketOptions=" + socketOptions +
                ", threadNum=" + threadNum +
                ", writeBufferSize=" + writeBufferSize +
                '}';
    }
}
