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

    public static final String BANNER = "\n" +
            "                               _                           _             _   \n" +
            "                              ( )_                        ( )           ( )_ \n" +
            "  ___   ___ ___     _ _  _ __ | ,_)     ___    _      ___ | |/')    __  | ,_)\n" +
            "/',__)/' _ ` _ `\\ /'_` )( '__)| |     /',__) /'_`\\  /'___)| , <   /'__`\\| |  \n" +
            "\\__, \\| ( ) ( ) |( (_| || |   | |_    \\__, \\( (_) )( (___ | |\\`\\ (  ___/| |_ \n" +
            "(____/(_) (_) (_)`\\__,_)(_)   `\\__)   (____/`\\___/'`\\____)(_) (_)`\\____)`\\__)";

    public static final String VERSION = "v1.3.23";
    private final boolean server;
    /**
     * 消息队列缓存大小
     */
    private int writeQueueSize = 0;
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
     * 服务器处理线程数
     */
    private int threadNum = Runtime.getRuntime().availableProcessors() + 1;
    private float limitRate = 0.9f;
    private float releaseRate = 0.6f;
    /**
     * 流控指标线
     */
    private int flowLimitLine = (int) (writeQueueSize * limitRate);
    /**
     * 释放流控指标线
     */
    private int releaseLine = (int) (writeQueueSize * releaseRate);
    /**
     * 是否启用控制台banner
     */
    private boolean bannerEnabled = true;
    /**
     * Socket 配置
     */
    private Map<SocketOption<Object>, Object> socketOptions;

    public IoServerConfig(boolean server) {
        this.server = server;
    }

    public final String getHost() {
        return host;
    }

    public final void setHost(String host) {
        this.host = host;
    }

    public final int getPort() {
        return port;
    }

    public final void setPort(int port) {
        this.port = port;
    }

    public final int getThreadNum() {
        return threadNum;
    }

    public final void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
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

    public final MessageProcessor<T> getProcessor() {
        return processor;
    }

    public final void setProcessor(MessageProcessor<T> processor) {
        this.processor = processor;
        if (processor instanceof NetMonitor) {
            this.monitor = (NetMonitor<T>) processor;
        }
    }

    public int getWriteQueueSize() {
        return writeQueueSize;
    }

    public void setWriteQueueSize(int writeQueueSize) {
        this.writeQueueSize = writeQueueSize;
        flowLimitLine = (int) (writeQueueSize * limitRate);
        releaseLine = (int) (writeQueueSize * releaseRate);
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    int getFlowLimitLine() {
        return flowLimitLine;
    }

    int getReleaseLine() {
        return releaseLine;
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
            socketOptions = new HashMap<>();
        }
        socketOptions.put(socketOption, f);
    }

    public boolean isServer() {
        return server;
    }

    @Override
    public String toString() {
        return "IoServerConfig{" +
                "writeQueueSize=" + writeQueueSize +
                ", readBufferSize=" + readBufferSize +
                ", host='" + host + '\'' +
                ", monitor=" + monitor +
                ", port=" + port +
                ", processor=" + processor +
                ", protocol=" + protocol +
                ", threadNum=" + threadNum +
                ", limitRate=" + limitRate +
                ", releaseRate=" + releaseRate +
                ", flowLimitLine=" + flowLimitLine +
                ", releaseLine=" + releaseLine +
                ", bannerEnabled=" + bannerEnabled +
                ", socketOptions=" + socketOptions +
                '}';
    }
}
