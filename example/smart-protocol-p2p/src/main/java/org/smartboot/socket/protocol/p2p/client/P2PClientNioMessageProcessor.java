package org.smartboot.socket.protocol.p2p.client;

import org.smartboot.ioc.MessageProcessor;
import org.smartboot.ioc.StateMachineEnum;
import org.smartboot.ioc.transport.NioSession;
import org.smartboot.socket.protocol.p2p.P2PNioSession;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;

public class P2PClientNioMessageProcessor implements MessageProcessor<BaseMessage> {

    private P2PNioSession session;

    @Override
    public void process(NioSession<BaseMessage> session, BaseMessage msg) {
//        System.out.println(msg);
        if (this.session.notifySyncMessage(msg)) {
            return;
        }
//        System.out.println(msg);
    }

    @Override
    public void stateEvent(NioSession<BaseMessage> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        System.err.println(stateMachineEnum);
        if (throwable != null) {
            throwable.printStackTrace();
        }
        switch (stateMachineEnum) {
            case NEW_SESSION:
                this.session = new P2PNioSession(session, 0);
                break;
        }
    }

    public P2PNioSession getSession() {
        return session;
    }
}
