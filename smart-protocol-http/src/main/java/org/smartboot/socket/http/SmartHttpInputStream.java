/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SmartHttpInputStream.java
 * Date: 2018-01-23 16:28:22
 * Author: sandao
 */

package org.smartboot.socket.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author 三刀
 * @version V1.0 , 2017/9/12
 */
public class SmartHttpInputStream extends InputStream {
    public BinaryBuffer binaryBuffer = new BinaryBuffer(1024);

    public int binWriteLength = 0;
    public int binReadLength = 0;
    private int contentLength;

    public SmartHttpInputStream(int contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public int read() throws IOException {
        if (binReadLength == contentLength) {
            return -1;
        }
        try {
            return binaryBuffer.take();
        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            binReadLength++;
        }
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (binReadLength >= contentLength) {
            return -1;
        }

        int i = 0;
        try {
            for (; i < len; i++) {
                if (binReadLength >= contentLength) {
                    break;
                }
                int c = read();
                b[off + i] = (byte) c;
            }
        } catch (IOException ee) {
        }
        return i;
    }

    @Override
    public int available() throws IOException {
        return binWriteLength < contentLength ? binaryBuffer.size() : 0;
    }

    public boolean append(byte b) {
        if (binWriteLength == contentLength) {
            return false;
        }
        try {
            binaryBuffer.put(b);
        } catch (InterruptedException e) {
            throw new RuntimeException("invalid content length");
        }
        binWriteLength++;
        return binWriteLength == contentLength;
    }
}
