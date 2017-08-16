package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.transport.IoSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 */
public abstract class AbstractAIOServerProcessor<T> implements ProtocolDataProcessor<T> {
    private static Logger logger = LogManager.getLogger(AbstractAIOServerProcessor.class);
    public static final String SESSION_PROCESS_THREAD = "_PROCESS_THREAD_";

    @Override
    public void init(int threadNum) {
    }

    @Override
    public boolean receive(IoSession<T> session, T entry) {
        try {
            session.getFilterChain().doProcessFilter(session, entry);
            process(session, entry);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void shutdown() {
    }

}
