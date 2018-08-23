package org.smartboot.socket.extension.processor;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.transport.AioSession;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public abstract class AbstractMessageProcessor<T> implements MessageProcessor<T>, NetMonitor<T> {

    private List<Plugin<T>> plugins = new ArrayList<>();

    @Override
    public final void readMonitor(AioSession<T> session, int readSize) {
        for (Plugin<T> plugin : plugins) {
            plugin.readMonitor(session, readSize);
        }
    }

    @Override
    public final void writeMonitor(AioSession<T> session, int writeSize) {
        for (Plugin<T> plugin : plugins) {
            plugin.writeMonitor(session, writeSize);
        }
    }

    @Override
    public final void process(AioSession<T> session, T msg) {
        boolean flag = true;
        for (Plugin<T> plugin : plugins) {
            if (!plugin.preProcess(session, msg)) {
                flag = false;
            }
        }
        if (flag) {
            process0(session, msg);
        }
    }

    public abstract void process0(AioSession<T> session, T msg);

    @Override
    public final void stateEvent(AioSession<T> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        for (Plugin<T> plugin : plugins) {
            plugin.stateEvent(stateMachineEnum, session, throwable);
        }
        stateEvent0(session, stateMachineEnum, throwable);
    }

    public abstract void stateEvent0(AioSession<T> session, StateMachineEnum stateMachineEnum, Throwable throwable);

    public final void addPlugin(Plugin plugin) {
        this.plugins.add(plugin);
    }
}
