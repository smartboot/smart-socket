package org.smartboot.socket.protocol;

import java.io.ByteArrayOutputStream;

/**
 * @author Seer
 * @version V1.0 , 2017/8/26
 */
public class BodyStream extends ByteArrayOutputStream {
    public static final String CRFL = "\r\n";

    int contentLength;

    /**
     * 返回true则表示Head结束
     *
     * @param data
     * @return
     */
    public boolean append(int data) {
        write(data);
        if (contentLength > 0 && count == contentLength) {
            return true;
        } else {
            return count >= 2 && buf[count - 1] == '\n' && buf[count - 2] == '\r';
        }
    }
}
