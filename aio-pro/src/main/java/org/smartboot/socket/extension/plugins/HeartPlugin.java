package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 心跳插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public abstract class HeartPlugin<T> extends AbstractPlugin<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartPlugin.class);
    private static Timer timer = new Timer("HeartMonitor Timer", true);
    private Map<AioSession<T>, Long> sessionMap = new HashMap<>();
    private int timeout;

    public HeartPlugin(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public final boolean preProcess(AioSession<T> session, T t) {
        sessionMap.put(session, System.currentTimeMillis());
        //是否心跳响应消息
        if (isHeartMessage(session, t)) {
            //延长心跳监测时间
            return false;
        }
        return true;
    }

    @Override
    public final void stateEvent(StateMachineEnum stateMachineEnum, AioSession<T> session, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                sessionMap.put(session, System.currentTimeMillis());
                registerHeart(session, timeout);
                //注册心跳监测
                break;
            case SESSION_CLOSED:
                //移除心跳监测
                sessionMap.remove(session);
                break;
        }
    }

    /**
     * 自定义心跳消息并发送
     *
     * @param session
     */
    public abstract void sendHeartRequest(AioSession<T> session) throws IOException;

    /**
     * 判断当前收到的消息是否为心跳消息。
     * 心跳请求消息与响应消息可能相同，也可能不同，因实际场景而异，故接口定义不做区分。
     *
     * @param session
     * @param msg
     * @return
     */
    public abstract boolean isHeartMessage(AioSession<T> session, T msg);

    private void registerHeart(final AioSession<T> session, final int timeout) {
        if (timeout <= 0) {
            LOGGER.info("sesssion:{} 因心跳超时时间为:{},终止启动心跳监测任务", session, timeout);
            return;
        }
        LOGGER.info("session:{}注册心跳任务,超时时间:{}", session, timeout);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (session.isInvalid()) {
                    sessionMap.remove(session);
                    LOGGER.info("session:{} 已失效，移除心跳任务", session);
                    return;
                }
                Long lastTime = sessionMap.get(session);
                if (lastTime == null) {
                    LOGGER.warn("session:{} timeout is null", session);
                    lastTime = System.currentTimeMillis();
                    sessionMap.put(session, lastTime);
                }
                if (System.currentTimeMillis() - lastTime > timeout) {
                    try {
                        sendHeartRequest(session);
                    } catch (IOException e) {
                        LOGGER.error("heart exception", e);
                    }
                }
                registerHeart(session, timeout);
            }
        }, timeout);
    }
}
