package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.MessageHandler;
import net.vinote.smart.socket.protocol.p2p.Session;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.process.AbstractClientDataProcessor;
import net.vinote.smart.socket.transport.IoSession;

public class P2PClientMessageProcessor extends AbstractClientDataProcessor<BaseMessage> {
    private P2pServiceMessageFactory serviceMessageFactory;

    private Session<BaseMessage> session;

    public P2PClientMessageProcessor(P2pServiceMessageFactory serviceMessageFactory) {
        this.serviceMessageFactory = serviceMessageFactory;
    }

    @Override
    public void process(IoSession<BaseMessage> ioSession, BaseMessage msg) throws Exception {
        if (session.notifySyncMessage(msg)) {
            return;
        }
        MessageHandler handler = serviceMessageFactory.getProcessor(msg.getClass());
        handler.handler(session, msg);
    }

    @Override
    public void initSession(IoSession<BaseMessage> ioSession) {
        session = new P2PSession(ioSession);
    }

    public Session<BaseMessage> getSession() {
        return session;
    }

}
