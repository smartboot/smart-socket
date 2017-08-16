package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.transport.IoSession;

/**
 * 客户端业务消息抽象处理器
 *
 * @param <T>
 * @author zhengjunwei
 */
public abstract class AbstractAioClientDataProcessor<T> implements ProtocolDataProcessor<T> {
//    private ClientDataProcessThread processThread;

    @Override
    public void init(int threadNum) {
//        processThread = new ClientDataProcessThread("ClientProcessor-Thread", this);
//        processThread.start();
    }

    @Override
    public boolean receive(IoSession<T> ioSession, T entry) {
        try {
            process(null, entry);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
