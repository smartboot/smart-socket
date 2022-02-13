/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: FastBufferThread.java
 * Date: 2020-07-02
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.buffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/11/16
 */
final class FastBufferThread extends Thread {
    private int pageIndex;

    public FastBufferThread(Runnable target, String name) {
        super(target, name);
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }
}
