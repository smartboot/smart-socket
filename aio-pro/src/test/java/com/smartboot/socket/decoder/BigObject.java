package com.smartboot.socket.decoder;

import java.io.InputStream;

/**
 * @author 三刀
 * @version V1.0 , 2018/12/22
 */
public class BigObject {
    private InputStream inputStream;

    public BigObject(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
