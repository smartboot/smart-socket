package net.vinote.smart.socket.protocol;

import java.io.ByteArrayInputStream;

/**
 * Created by zhengjunwei on 2017/6/25.
 */
public class BodyInputStream extends ByteArrayInputStream {

    public BodyInputStream(byte[] buf) {
        super(buf);
    }

}
