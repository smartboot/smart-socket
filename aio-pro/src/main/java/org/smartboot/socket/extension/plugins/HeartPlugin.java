/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HeartPlugin.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.QuickTimerTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * 心跳插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public abstract class HeartPlugin<T> extends AbstractPlugin<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartPlugin.class);
    private static final TimeoutCallback DEFAULT_TIMEOUT_CALLBACK = new TimeoutCallback() {
        @Override
        public void callback(AioSession session, long lastTime) {
            session.close(true);
        }
    };
    private Map<AioSession, Long> sessionMap = new HashMap<>();
    /**
     * 心跳频率
     */
    private long heartRate;
    /**
     * 在超时时间内未收到消息,关闭连接。
     */
    private long timeout;
    private TimeoutCallback timeoutCallback;

    /**
     * 心跳插件
     *
     * @param heartRate 心跳触发频率
     * @param timeUnit  heatRate单位
     */
    public HeartPlugin(int heartRate, TimeUnit timeUnit) {
        this(heartRate, 0, timeUnit);
    }

    /**
     * 心跳插件
     * <p>
     * 心跳插件在断网场景可能会触发TCP Retransmission,导致无法感知到网络实际状态,可通过设置timeout关闭连接
     * </p>
     *
     * @param heartRate 心跳触发频率
     * @param timeout   消息超时时间
     * @param unit      时间单位
     */
    public HeartPlugin(int heartRate, int timeout, TimeUnit unit) {
        this(heartRate, timeout, unit, DEFAULT_TIMEOUT_CALLBACK);
    }

    /**
     * 心跳插件
     * <p>
     * 心跳插件在断网场景可能会触发TCP Retransmission,导致无法感知到网络实际状态,可通过设置timeout关闭连接
     * </p>
     *
     * @param heartRate 心跳触发频率
     * @param timeout   消息超时时间
     */
    public HeartPlugin(int heartRate, int timeout, TimeUnit timeUnit, TimeoutCallback timeoutCallback) {
        if (timeout > 0 && heartRate >= timeout) {
            throw new IllegalArgumentException("heartRate must little then timeout");
        }
        this.heartRate = timeUnit.toMillis(heartRate);
        this.timeout = timeUnit.toMillis(timeout);
        this.timeoutCallback = timeoutCallback;
    }

    @Override
    public final boolean preProcess(AioSession session, T t) {
        sessionMap.put(session, System.currentTimeMillis());
        //是否心跳响应消息
        if (isHeartMessage(session, t)) {
            //延长心跳监测时间
            return false;
        }
        return true;
    }

    @Override
    public final void stateEvent(StateMachineEnum stateMachineEnum, AioSession session, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                sessionMap.put(session, System.currentTimeMillis());
                registerHeart(session, heartRate);
                //注册心跳监测
                break;
            case SESSION_CLOSED:
                //移除心跳监测
                sessionMap.remove(session);
                break;
            default:
                break;
        }
    }

    /**
     * 自定义心跳消息并发送
     *
     * @param session
     * @throws IOException
     */
    public abstract void sendHeartRequest(AioSession session) throws IOException;

    /**
     * 判断当前收到的消息是否为心跳消息。
     * 心跳请求消息与响应消息可能相同，也可能不同，因实际场景而异，故接口定义不做区分。
     *
     * @param session
     * @param msg
     * @return
     */
    public abstract boolean isHeartMessage(AioSession session, T msg);

    private void registerHeart(final AioSession session, final long heartRate) {
        if (heartRate <= 0) {
            LOGGER.info("sesssion:{} 因心跳超时时间为:{},终止启动心跳监测任务", session, heartRate);
            return;
        }
        LOGGER.debug("session:{}注册心跳任务,超时时间:{}", session, heartRate);
        QuickTimerTask.SCHEDULED_EXECUTOR_SERVICE.schedule(new TimerTask() {
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
                long current = System.currentTimeMillis();
                //超时未收到消息，关闭连接
                if (timeout > 0 && (current - lastTime) > timeout) {
                    timeoutCallback.callback(session, lastTime);
                }
                //超时未收到消息,尝试发送心跳消息
                else if (current - lastTime > heartRate) {
                    try {
                        sendHeartRequest(session);
                        session.writeBuffer().flush();
                    } catch (IOException e) {
                        LOGGER.error("heart exception,will close session:{}", session, e);
                        session.close(true);
                    }
                }
                registerHeart(session, heartRate);
            }
        }, heartRate, TimeUnit.MILLISECONDS);
    }

    public interface TimeoutCallback {
        public void callback(AioSession session, long lastTime);
    }
}
