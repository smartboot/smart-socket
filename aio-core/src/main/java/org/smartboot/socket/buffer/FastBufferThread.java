/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: FastBufferThread.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.buffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/11/16
 */
final class FastBufferThread extends Thread {
    /**
     * 索引标识
     */
    private final int index;

    FastBufferThread(Runnable target, String name, int index) {
        super(target, name + index);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
