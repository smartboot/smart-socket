package org.smartboot.socket.transport;

/**
 * @author 三刀
 * @version V1.0 , 2018/9/24
 */
interface Function<F, T> {
    /**
     * 类似于JDK8 的 Function功能
     *
     * @param var
     * @return
     */
    T apply(F var);
}
