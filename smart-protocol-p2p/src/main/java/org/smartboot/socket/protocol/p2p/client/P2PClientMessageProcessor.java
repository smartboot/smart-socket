package org.smartboot.socket.protocol.p2p.client;

import org.smartboot.socket.protocol.p2p.P2PSession;
import org.smartboot.socket.protocol.p2p.MessageHandler;
import org.smartboot.socket.protocol.p2p.Session;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.service.process.MessageProcessor;
import org.smartboot.socket.transport.AioSession;

public class P2PClientMessageProcessor implements MessageProcessor<BaseMessage> {
    private P2pServiceMessageFactory serviceMessageFactory;

    private Session<BaseMessage> session;

    public P2PClientMessageProcessor(P2pServiceMessageFactory serviceMessageFactory) {
        this.serviceMessageFactory = serviceMessageFactory;
    }

    @Override
    public void process(AioSession<BaseMessage> ioSession, BaseMessage msg) throws Exception {
        if (session.notifySyncMessage(msg)) {
            return;
        }
        MessageHandler handler = serviceMessageFactory.getProcessor(msg.getClass());
        handler.handler(session, msg);
    }

    @Override
    public void initSession(AioSession<BaseMessage> ioSession) {
        session = new P2PSession(ioSession);
    }

    public Session<BaseMessage> getSession() {
        return session;
    }

}
