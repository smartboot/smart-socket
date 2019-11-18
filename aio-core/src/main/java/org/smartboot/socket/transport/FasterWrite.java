package org.smartboot.socket.transport;

import org.smartboot.socket.buffer.VirtualBuffer;

/**
 * 快速write
 *
 * @author 三刀
 * @version V1.0 , 2019/11/7
 */
class FasterWrite {
    /**
     * 申请数据输出信号量
     *
     * @return true:申请成功,false:申请失败
     */
    boolean tryAcquire() {
        return false;
    }

    /**
     * 执行数据输出
     *
     * @param buffer 待输出数据
     */
    void write(VirtualBuffer buffer) {
    }

}
