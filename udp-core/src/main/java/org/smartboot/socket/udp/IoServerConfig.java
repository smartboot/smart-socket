/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IoServerConfig.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.udp;


/**
 * Quickly服务端/客户端配置信息 T:解码后生成的对象类型
 *
 * @author 三刀
 * @version V1.0.0
 */
final class IoServerConfig<Request, Response> {


    /**
     * 消息体缓存大小,字节
     */
    private int readBufferSize = 512;

    /**
     * 消息处理器
     */
    private MessageProcessor<Request, Response> processor;
    /**
     * 协议编解码
     */
    private Protocol<Request, Response> protocol;

    /**
     * 线程数
     */
    private int threadNum = 1;


    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }


    public Protocol<Request, Response> getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol<Request, Response> protocol) {
        this.protocol = protocol;
    }

    public final MessageProcessor<Request, Response> getProcessor() {
        return processor;
    }

    public final void setProcessor(MessageProcessor<Request, Response> processor) {
        this.processor = processor;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }


    @Override
    public String toString() {
        return "IoServerConfig{" +
                ", readBufferSize=" + readBufferSize +
                ", processor=" + processor +
                ", protocol=" + protocol +
                '}';
    }
}
