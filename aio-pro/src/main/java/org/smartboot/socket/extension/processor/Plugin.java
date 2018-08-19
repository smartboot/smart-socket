package org.smartboot.socket.extension.processor;

import org.smartboot.socket.Filter;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public interface Plugin<T> extends Filter<T> {

    /**
     * @param session
     * @param t
     * @return
     */
    boolean preProcess(AioSession<T> session, T t);


    /**
     * @param stateMachineEnum
     * @param session
     * @param throwable
     */
    void stateEvent(StateMachineEnum stateMachineEnum, AioSession<T> session, Throwable throwable);

}
