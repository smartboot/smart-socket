package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.transport.IoSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 客户端业务消息抽象处理器
 *
 * @param <T>
 * @author zhengjunwei
 */
public abstract class AbstractClientDataProcessor<T> implements ProtocolDataProcessor<T> {
    private ClientDataProcessThread processThread;

    @Override
    public void init(int threadNum) {
        processThread = new ClientDataProcessThread("ClientProcessor-Thread", this);
        processThread.start();
    }

    @Override
    public boolean receive(IoSession<T> ioSession, T entry) {
        Session<T> session = ioSession.getServiceSession();
        if (!session.notifySyncMessage(entry)) {
            // 同步响应消息若出现超时情况,也会进到if里面
            processThread.put(entry);
        }
        return true;
    }

    @Override
    public void shutdown() {
        processThread.shutdown();
    }

    /**
     * 业务消息处理线程
     *
     * @author Seer
     */
    class ClientDataProcessThread extends Thread {
        private Logger logger = LogManager.getLogger(ClientDataProcessThread.class);
        private ArrayBlockingQueue<T> list = new ArrayBlockingQueue<T>(1024);
        protected volatile boolean running = true;

        public ClientDataProcessThread(String name, ProtocolDataProcessor<T> processor) {
            super(name);
        }

        public void put(T msg) {
            try {
                list.put(msg);
            } catch (InterruptedException e) {
                logger.warn("", e);
            }
        }

        @Override
        public void run() {

            while (running) {
                try {
                    T msg = list.take();
                    AbstractClientDataProcessor.this.process(null, msg);
                } catch (Exception e) {
                    if (running) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }

        /**
         * 停止消息处理线程
         */
        public void shutdown() {
            running = false;
            this.interrupt();
        }
    }
}
