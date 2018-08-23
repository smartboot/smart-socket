package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.nio.channels.AsynchronousChannelGroup;

/**
 * 断链重连插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
class ReconnectPlugin<T> extends AbstractPlugin<T> {

    private AioQuickClient<T> client;

    private boolean shutdown = false;

    private AsynchronousChannelGroup asynchronousChannelGroup;

    public ReconnectPlugin(AioQuickClient<T> client) {
        this(client, null);
    }

    public ReconnectPlugin(AioQuickClient<T> client, AsynchronousChannelGroup asynchronousChannelGroup) {
        this.client = client;
        this.asynchronousChannelGroup = asynchronousChannelGroup;
    }

    @Override
    public void stateEvent(StateMachineEnum stateMachineEnum, AioSession<T> session, Throwable throwable) {
        if (stateMachineEnum != StateMachineEnum.SESSION_CLOSED || shutdown) {
            return;
        }
        try {
            if (asynchronousChannelGroup == null) {
                client.start();
            } else {
                client.start(asynchronousChannelGroup);
            }
        } catch (Exception e) {
            shutdown = true;
            e.printStackTrace();
        }

    }

    public void shutdown() {
        shutdown = true;
    }
}
