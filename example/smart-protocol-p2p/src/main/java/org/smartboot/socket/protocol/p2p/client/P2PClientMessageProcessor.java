package org.smartboot.socket.protocol.p2p.client;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.protocol.p2p.MessageHandler;
import org.smartboot.socket.protocol.p2p.P2PSession;
import org.smartboot.socket.protocol.p2p.Session;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.transport.AioSession;

public class P2PClientMessageProcessor extends AbstractMessageProcessor<BaseMessage> {
    private P2pServiceMessageFactory serviceMessageFactory;

    private Session<BaseMessage> session;

    public P2PClientMessageProcessor(P2pServiceMessageFactory serviceMessageFactory) {
        this.serviceMessageFactory = serviceMessageFactory;
    }

    @Override
    public void process0(AioSession<BaseMessage> ioSession, BaseMessage msg) {
        if (session.notifySyncMessage(msg)) {
            return;
        }
        MessageHandler handler = serviceMessageFactory.getProcessor(msg.getClass());
        handler.handler(session, msg);
    }

    @Override
    public void stateEvent0(AioSession<BaseMessage> ioSession, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                session = new P2PSession(ioSession);
                break;
        }

    }

    public Session<BaseMessage> getSession() {
        return session;
    }

}
