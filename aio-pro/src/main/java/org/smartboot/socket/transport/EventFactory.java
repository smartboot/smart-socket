package org.smartboot.socket.transport;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/11
 */
interface EventFactory<T> {
    /**
     * 实例化对象
     *
     * @return
     */
    T newInstance();

    /**
     * 重置对象
     *
     * @param entity
     */
    void restEntity(T entity);
}
