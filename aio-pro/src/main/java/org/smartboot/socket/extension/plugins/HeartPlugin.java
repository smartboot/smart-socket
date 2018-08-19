package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

/**
 * 心跳插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public class HeartPlugin<T> implements Plugin<T> {
    @Override
    public boolean preProcess(AioSession<T> session, T t) {

        return true;
    }

    @Override
    public void stateEvent(StateMachineEnum stateMachineEnum, AioSession<T> session, Throwable throwable) {
        switch (stateMachineEnum){
            case NEW_SESSION:
                //注册心跳监测
                break;
            case SESSION_CLOSED:
                //移除心跳监测
                break;
        }
    }

    @Override
    public void readFilter(AioSession<T> session, int readSize) {

    }

    @Override
    public void writeFilter(AioSession<T> session, int writeSize) {

    }
}
