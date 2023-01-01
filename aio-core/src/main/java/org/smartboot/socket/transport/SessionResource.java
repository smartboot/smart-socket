package org.smartboot.socket.transport;

import org.smartboot.socket.buffer.BufferPagePool;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/11/27
 */
abstract class SessionResource {
    /**
     * 客户端服务配置。
     * <p>调用AioQuickClient的各setXX()方法，都是为了设置config的各配置项</p>
     */
    protected final IoServerConfig config = new IoServerConfig();

    /**
     * 内存池
     */
    protected BufferPagePool bufferPool = null;
}
