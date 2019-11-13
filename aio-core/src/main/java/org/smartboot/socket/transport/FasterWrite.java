package org.smartboot.socket.transport;

import org.smartboot.socket.buffer.VirtualBuffer;

/**
 * 快速write
 *
 * @author 三刀
 * @version V1.0 , 2019/11/7
 */
class FasterWrite {
    boolean tryAcquire() {
        return false;
    }


    void write(VirtualBuffer buffer) {
    }

}
