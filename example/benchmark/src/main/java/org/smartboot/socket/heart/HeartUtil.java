/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HeartUtil.java
 * Date: 2020-05-02
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.heart;

import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2020/5/2
 */
public class HeartUtil {
    private static final String HEART_BEAT_MSG = "heartbeat";

    public static void sendHeartMessage(AioSession session) throws IOException {
        sendMessage(session, HEART_BEAT_MSG);
    }

    public static void sendMessage(AioSession session, String msg) throws IOException {
        WriteBuffer writeBuffer = session.writeBuffer();
        byte[] heartBytes = msg.getBytes();
        writeBuffer.writeInt(heartBytes.length);
        writeBuffer.write(heartBytes);
        writeBuffer.flush();
    }

    public static boolean isHeartMessage(String msg) {
        return HEART_BEAT_MSG.equals(msg);
    }

}
