package org.smartboot.socket.buffer.ring;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/11
 */
public interface EventFactory<T> {
    T newInstance();

    void restEntity(T entity);
}
