package org.smartboot.socket.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ByteBuffer内存池
 *
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public class BufferPagePool {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPagePool.class);
    private BufferPage[] bufferPageList;
    /**
     * 内存页游标
     */
    private int cursor = -1;

    /**
     * @param pageSize 内存页大小
     * @param poolSize 内存页个数
     * @param isDirect 是否使用直接缓冲区
     */
    public BufferPagePool(final int pageSize, final int poolSize, final boolean isDirect) {
        bufferPageList = new BufferPage[poolSize];
        for (int i = 0; i < poolSize; i++) {
            bufferPageList[i] = new BufferPage(pageSize, isDirect);
        }
//        Timer timer = new Timer("Quick Timer", true);
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    for (BufferPage p : bufferPageList) {
//                        LOGGER.info(p.toString());
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }, 0, 5000);
    }

    /**
     * 申请内存页
     *
     * @return
     */
    public BufferPage allocateBufferPage() {
        //轮训游标，均衡分配内存页
        cursor = (cursor + 1) % bufferPageList.length;
        BufferPage page = bufferPageList[cursor];
        return page;
    }

}
