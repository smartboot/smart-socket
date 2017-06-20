package net.vinote.smart.socket.transport.nio;

import net.vinote.smart.socket.exception.StatusException;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.transport.ChannelService;
import net.vinote.smart.socket.transport.enums.ChannelServiceStatusEnum;
import net.vinote.smart.socket.transport.enums.SessionStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Seer
 * @version AbstractChannelService.java, v 0.1 2015年3月19日 下午6:57:01 Seer Exp.
 */
abstract class AbstractChannelService<T> implements ChannelService {
    private Logger logger = LogManager.getLogger(AbstractChannelService.class);
    /**
     * 服务状态
     */
    volatile ChannelServiceStatusEnum status = ChannelServiceStatusEnum.Init;

    /**
     * 服务配置
     */
    QuicklyConfig<T> config;

    Selector selector;

    /**
     * 传输层Channel服务处理线程
     */
    Thread serverThread;

    /**
     * 读管道单论操作读取次数
     */
    final int READ_LOOP_TIMES;

    public AbstractChannelService(final QuicklyConfig<T> config) {
        this.config = config;
        READ_LOOP_TIMES = config.getReadLoopTimes();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    public final void run() {
        updateServiceStatus(ChannelServiceStatusEnum.RUNING);
        // 通过检查状态使之一直保持服务状态
        while (ChannelServiceStatusEnum.RUNING == status) {
            try {
                running();
            } catch (ClosedSelectorException e) {
                updateServiceStatus(ChannelServiceStatusEnum.Abnormal);// Selector关闭触发服务终止
            } catch (Exception e) {
                exceptionInSelector(e);
            }
        }
        updateServiceStatus(ChannelServiceStatusEnum.STOPPED);
        logger.info("Channel is stop!");
    }

    /**
     * 运行channel服务
     *
     * @throws IOException
     * @throws Exception
     */
    private void running() throws IOException, Exception {
        // 优先获取SelectionKey,若无关注事件触发则阻塞在selector.select(),减少select被调用次数
        Set<SelectionKey> keySet = selector.selectedKeys();
        if (keySet.isEmpty()) {
            selector.select();
        }
        Iterator<SelectionKey> keyIterator = keySet.iterator();
        // 执行本次已触发待处理的事件
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            NioAttachment attach = null;
            try {
                attach = (NioAttachment) key.attachment();
                // 读取客户端数据
                if (key.isReadable()) {
                    attach.setCurSelectionOP(SelectionKey.OP_READ);
                    readFromChannel(key, attach);
                }/* else if (key.isWritable()) {// 输出数据至客户端
                    attach.setCurSelectionOP(SelectionKey.OP_WRITE);
					writeToChannel(key, attach);
				}*/ else if (key.isAcceptable() || key.isConnectable()) {// 建立新连接,Client触发Connect,Server触发Accept
                    acceptConnect(key, selector);
                } else {
                    logger.warn("奇怪了...");
                }
            } catch (Exception e) {
                exceptionInSelectionKey(key, e);
            } finally {
                // 移除已处理的事件
                keyIterator.remove();
                if (attach != null)
                    attach.setCurSelectionOP(0);
            }
        }
    }

    /**
     * 从管道流中读取数据
     *
     * @param key
     * @param attach
     * @throws IOException
     */
    private void readFromChannel(SelectionKey key, NioAttachment attach) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        NioSession<?> session = attach.getSession();
        int readSize = 0;
        int loopTimes = READ_LOOP_TIMES;// 轮训次数,以便及时让出资源
        while ((readSize = socketChannel.read(session.flushReadBuffer())) > 0 && --loopTimes > 0)
            ;// 读取管道中的数据块
        // 达到流末尾则注销读关注
        if (readSize == -1 || session.getStatus() == SessionStatusEnum.CLOSING) {
            session.cancelReadAttention();
            // key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
            if (session.getWriteBuffer() == null || key.isValid()) {
                session.close();
                logger.info("关闭Socket[" + socketChannel + "]");
            } else {
                logger.info("注销Socket[" + socketChannel + "]读关注");
            }
        }
    }

    /**
     * 接受并建立Socket连接
     *
     * @param key
     * @param selector
     * @throws IOException
     */
    abstract void acceptConnect(SelectionKey key, Selector selector) throws IOException;

    /**
     * 判断状态是否有异常
     */
    final void assertAbnormalStatus() {
        if (status == ChannelServiceStatusEnum.Abnormal) {
            throw new StatusException("channel service's status is abnormal");
        }
    }

    /**
     * 处理某个已触发且发生了异常的SelectionKey
     *
     * @param key
     * @param e
     * @throws Exception
     */
    abstract void exceptionInSelectionKey(SelectionKey key, Exception e) throws Exception;

    /**
     * 处理选择器层面的异常,此时基本上会导致当前的链路不再可用
     *
     * @param e
     */
    abstract void exceptionInSelector(Exception e);

    /**
     * 更新服务状态
     *
     * @param status
     */
    final void updateServiceStatus(final ChannelServiceStatusEnum status) {
        this.status = status;
        notifyWhenUpdateStatus(status);
    }

    /**
     * 服务启动检测, 校验服务器的基本配置是否正常
     */
    void checkStart() {
        if (config == null) {
            throw new NullPointerException(getClass().getSimpleName() + "'s config is null");
        }
        if (config.getProtocolFactory() == null) {
            throw new NullPointerException(QuicklyConfig.class.getSimpleName() + "'s protocolFactory is null");
        }

        if (config.getProcessor() == null) {
            throw new NullPointerException(QuicklyConfig.class.getSimpleName() + "'s receiver is null");
        }

    }

    protected void notifyWhenUpdateStatus(final ChannelServiceStatusEnum status) {

    }

}
