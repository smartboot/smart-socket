package org.smartboot.socket.protocol.p2p.server;

import org.smartboot.socket.protocol.p2p.MessageHandler;
import org.smartboot.socket.protocol.p2p.P2PSession;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.service.process.MessageProcessor;
import org.smartboot.socket.transport.AioSession;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 */
public final class P2PServerMessageProcessor implements MessageProcessor<BaseMessage> {
    private P2pServiceMessageFactory serviceMessageFactory;

    public P2PServerMessageProcessor(P2pServiceMessageFactory serviceMessageFactory) {
        this.serviceMessageFactory = serviceMessageFactory;
    }

    @Override
    public void process(AioSession<BaseMessage> ioSession, BaseMessage entry) {
        P2PSession session = ioSession.getAttribute(P2PSession.SESSION_KEY);
        if (session.notifySyncMessage(entry)) {
            return;
        }
        MessageHandler handler = serviceMessageFactory.getProcessor(entry.getClass());
        handler.handler(session, entry);
    }

    @Override
    public void initSession(AioSession<BaseMessage> session) {
        session.setAttribute(P2PSession.SESSION_KEY, new P2PSession(session));
    }

}
