package net.vinote.smart.socket.protocol.p2p.server;

import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.MessageHandler;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.process.AbstractAIOServerProcessor;
import net.vinote.smart.socket.service.process.AbstractServerDataGroupProcessor;
import net.vinote.smart.socket.transport.IoSession;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 */
public final class P2PServerMessageProcessor extends AbstractAIOServerProcessor<BaseMessage> {
    private P2pServiceMessageFactory serviceMessageFactory;

    public P2PServerMessageProcessor(P2pServiceMessageFactory serviceMessageFactory) {
        this.serviceMessageFactory = serviceMessageFactory;
    }

    @Override
    public void process(IoSession<BaseMessage> ioSession, BaseMessage entry) throws Exception {
        P2PSession session = ioSession.getAttribute(P2PSession.SESSION_KEY);
        if (session.notifySyncMessage(entry)) {
            return;
        }
        MessageHandler handler = serviceMessageFactory.getProcessor(entry.getClass());
        handler.handler(session, entry);
    }

    @Override
    public void initSession(IoSession<BaseMessage> session) {
        session.setAttribute(P2PSession.SESSION_KEY, new P2PSession(session));
    }
}
