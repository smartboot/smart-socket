package org.smartboot.socket.transport;

/**
 * @author 三刀
 * @version V1.0 , 2018/9/24
 */
public interface Function<F, T> {
    T apply(F var);
}
