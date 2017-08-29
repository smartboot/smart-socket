package org.smartboot.socket.protocol;

import java.io.ByteArrayOutputStream;

/**
 * @author Seer
 * @version V1.0 , 2017/8/26
 */
public class DataStream extends ByteArrayOutputStream {
    public static final String CRFL = "\r\n";
    private byte[] endFLag;
    int contentLength;

    public DataStream(byte[] endFLag) {
        this.endFLag = endFLag;
    }

    /**
     * 返回true则表示Head结束
     *
     * @param data
     * @return
     */
    public boolean append(int data) {
        write(data);
        if (contentLength > 0) {
            return count == contentLength;
        }

        if (count < endFLag.length) {
            return false;
        }
        for (int i = 1; i <= endFLag.length; i++) {
            if (buf[count - i] != endFLag[endFLag.length - i]) {
                return false;
            }
        }
        return true;
    }
}
