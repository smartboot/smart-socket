package org.smartboot.socket.transport;

/**
 * @param <F> the type of the input to the function
 * @param <T> the type of the result of the function
 * @author 三刀
 * @version V1.0 , 2018/9/24
 */
interface Function<F, T> {
    /**
     * 类似于JDK8 的 Function功能
     *
     * @param var the function argument
     * @return the function result
     */
    T apply(F var);
}
