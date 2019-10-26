package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 黑名单插件,smart-socket会拒绝与黑名单中的IP建立连接
 *
 * @author 三刀
 * @version V1.0 , 2019/3/27
 */
public final class BlackListPlugin<T> extends AbstractPlugin<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlackListPlugin.class);
    private ConcurrentLinkedQueue<BlackListRule> ipBlackList = new ConcurrentLinkedQueue<>();

    @Override
    public boolean shouldAccept(AsynchronousSocketChannel channel) {
        InetSocketAddress inetSocketAddress = null;
        try {
            inetSocketAddress = (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
            LOGGER.error("get remote address error.", e);
        }
        if (inetSocketAddress == null) {
            return true;
        }
        for (BlackListRule rule : ipBlackList) {
            if (!rule.access(inetSocketAddress)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 添加黑名单失败规则
     *
     * @param rule
     */
    public void addRule(BlackListRule rule) {
        ipBlackList.add(rule);
    }

    /**
     * 移除黑名单规则
     *
     * @param rule
     */
    public void removeRule(BlackListRule rule) {
        ipBlackList.remove(rule);
    }

    /**
     * 黑名单规则定义
     */
    public interface BlackListRule {
        /**
         * 是否允许建立连接
         *
         * @param address
         * @return
         */
        boolean access(InetSocketAddress address);
    }
}
