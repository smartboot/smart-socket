/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BlackListPlugin.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 黑名单插件,smart-socket会拒绝与黑名单中的IP建立连接
 *
 * @author 三刀
 * @version V1.0 , 2019/3/27
 * @deprecated 该插件不太常用，且自行实现的难度也不大。
 */
public final class BlackListPlugin<T> extends AbstractPlugin<T> {
    private ConcurrentLinkedQueue<BlackListRule> ipBlackList = new ConcurrentLinkedQueue<>();

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        InetSocketAddress inetSocketAddress = null;
        try {
            inetSocketAddress = (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inetSocketAddress == null) {
            return channel;
        }
        for (BlackListRule rule : ipBlackList) {
            if (!rule.access(inetSocketAddress)) {
                return null;
            }
        }
        return channel;
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
