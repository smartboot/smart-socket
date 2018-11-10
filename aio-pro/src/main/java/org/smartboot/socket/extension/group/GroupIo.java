package org.smartboot.socket.extension.group;

import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/9
 */
public interface GroupIo<T> {
    /**
     * 将AioSession加入群组group
     *
     * @param group
     * @param session
     */
    void join(String group, AioSession<T> session);


    /**
     * 将AioSession从群众group中移除
     *
     * @param group
     * @param session
     */
    void remove(String group, AioSession<T> session);

    /**
     * AioSession从所有群组中退出
     *
     * @param session
     */
    void remove(AioSession<T> session);

    /**
     * 群发消息
     *
     * @param group
     * @param t
     */
    void writeToGroup(String group, byte[] t);
}
