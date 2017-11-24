package net.vinote.demo;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.StateMachineEnum;

/**
 * @author 三刀
 * @version V1.0 , 2017/8/23
 */
public class IntegerClientProcessor implements MessageProcessor<Integer> {
    private AioSession<Integer> session;

    @Override
    public void process(AioSession<Integer> session, Integer msg) {
        System.out.println("receive data from server：" + msg);
    }

    @Override
    public void stateEvent(AioSession<Integer> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                this.session = session;
                break;
            default:
                System.out.println("other state:" + stateMachineEnum);
        }

    }

    public AioSession<Integer> getSession() {
        return session;
    }
}
