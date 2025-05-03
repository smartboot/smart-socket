/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BufferPage.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.buffer;


/**
 * ByteBuffer内存页
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
interface BufferPage {


    /**
     * 申请虚拟内存
     *
     * @param size 申请大小
     * @return 虚拟内存对象
     */
    VirtualBuffer allocate(final int size);

}
