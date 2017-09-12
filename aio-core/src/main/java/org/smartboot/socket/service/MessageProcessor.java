package org.smartboot.socket.service;

import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StateMachineEnum;

/**
 * 消息处理器
 *
 * @author Seer
 * @version MessageProcessor.java, v 0.1 2015年3月13日 下午3:26:55 Seer Exp.
 */
public interface MessageProcessor<T> {

    /**
     * 用于处理指定session内的一个消息实例,若直接在该方法内处理消息,则实现的是同步处理方式.
     * 若需要采用异步，则介意此方法的实现仅用于接收消息，至于消息处理则在其他线程中实现
     *
     * @param session
     * @throws Exception
     */
    public void process(AioSession<T> session, T msg);

    /**
     * 状态机事件,当枚举事件发生时会触发该方法
     * @param session
     */
    void stateEvent(AioSession<T> session, StateMachineEnum stateMachineEnum);
}
