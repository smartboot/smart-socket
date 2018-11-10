package org.smartboot.socket.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public class BufferPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPool.class);
    private BufferPage[] bufferPageList;

    private int cursor = -1;

    private int pageSize;

    public BufferPool(int pageSize, int poolSize, boolean isDirect) {
        this.pageSize = pageSize;
        bufferPageList = new BufferPage[poolSize];
        for (int i = 0; i < poolSize; i++) {
            bufferPageList[i] = new BufferPage(this, pageSize, isDirect);
        }
    }

    public BufferPage getBufferPage() {
        for (int i = 1; i < bufferPageList.length; i++) {
            cursor = (i + cursor) % bufferPageList.length;
            BufferPage page = bufferPageList[cursor];
            if (page.hasFree()) {
                return page;
            }
        }
        LOGGER.debug("create temp bufferPage");
        //生成临时Page,内存管理交由JVM
        return new BufferPage(null, pageSize, false);
    }
}
