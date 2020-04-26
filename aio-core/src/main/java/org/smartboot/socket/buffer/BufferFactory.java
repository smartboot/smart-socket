/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BufferFactory.java
 * Date: 2020-04-07
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.buffer;

/**
 * 内存池工厂
 *
 * @author 三刀
 * @version V1.0 , 2020/4/7
 */
public interface BufferFactory {
    /**
     * 禁用状态的内存池
     */
    BufferFactory DISABLED_BUFFER_FACTORY = () -> new BufferPagePool.NoneBufferPagePool();

    /**
     * 创建内存池
     *
     * @return
     */
    BufferPagePool create();
}
