package org.smartboot.socket.mqtt;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public interface VariableHeader {

    void  decodeVariableHeader(ByteBuffer buffer);
}
