package org.smartboot.socket.protocol;

import java.io.ByteArrayOutputStream;

/**
 * @author Seer
 * @version V1.0 , 2017/8/26
 */
public class HeadStream extends ByteArrayOutputStream {
    public static final String CRFL = "\r\n";

    /**
     * 返回true则表示Head结束
     *
     * @param data
     * @return
     */
    public boolean append(int data) {
        write(data);
        return count >= 4 && buf[count - 1] == '\n' && buf[count - 2] == '\r' && buf[count - 3] == '\n' && buf[count - 4] == '\r';
    }
}
