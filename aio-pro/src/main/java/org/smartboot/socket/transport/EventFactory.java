/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: EventFactory.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

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
