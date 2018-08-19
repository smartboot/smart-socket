package org.smartboot.socket.extension.processor;

import org.smartboot.socket.Filter;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public abstract class AbstractMessageProcessor<T> implements MessageProcessor<T>, Filter<T> {

    private List<Plugin<T>> plugins = new ArrayList<>();

    @Override
    public void readFilter(AioSession<T> session, int readSize) {
        for (Plugin<T> plugin : plugins) {
            plugin.readFilter(session, readSize);
        }
    }

    @Override
    public void writeFilter(AioSession<T> session, int writeSize) {
        for (Plugin<T> plugin : plugins) {
            plugin.writeFilter(session, writeSize);
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
        switch (stateMachineEnum) {
            case PROCESS_EXCEPTION:
            case INPUT_EXCEPTION:
            case OUTPUT_EXCEPTION:
                for (Plugin<T> plugin : plugins) {
                    plugin.doException(stateMachineEnum, session, throwable);
                }
                break;
            case NEW_SESSION:
            case SESSION_CLOSING:
            case SESSION_CLOSED:
            case FLOW_LIMIT:
            case RELEASE_FLOW_LIMIT:
            case INPUT_SHUTDOWN:
                for (Plugin<T> plugin : plugins) {
                    plugin.doState(stateMachineEnum, session);
                }
                break;
        }
        stateEvent0(session, stateMachineEnum, throwable);
    }

    public abstract void stateEvent0(AioSession<T> session, StateMachineEnum stateMachineEnum, Throwable throwable);

    public void addPlugin(Plugin plugin) {
        this.plugins.add(plugin);
    }
}
