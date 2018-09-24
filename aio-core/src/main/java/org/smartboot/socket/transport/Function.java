package org.smartboot.socket.transport;

/**
 * @author 三刀
 * @version V1.0 , 2018/9/24
 */
interface Function<F, T> {
    T apply(F var);
}
