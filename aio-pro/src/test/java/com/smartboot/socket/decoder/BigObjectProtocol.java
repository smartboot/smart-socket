package com.smartboot.socket.decoder;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.DecoderException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * s
 *
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class BigObjectProtocol implements Protocol<BigObject> {

    @Override
    public BigObject decode(ByteBuffer readBuffer, AioSession<BigObject> session) {
        if (readBuffer.remaining() < 4) {
            return null;
        }
        int fileSize = readBuffer.getInt();
        try {
            InputStream inputStream = session.getInputStream(fileSize);
            BigObject object = new BigObject(inputStream);
            return object;
        } catch (IOException e) {
            throw new DecoderException(e);
        }
    }
}
