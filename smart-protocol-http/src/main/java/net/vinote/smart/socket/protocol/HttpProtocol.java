package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.transport.TransportSession;

import java.nio.ByteBuffer;

/**
 * Created by zhengjunwei on 2017/6/20.
 */
public class HttpProtocol implements Protocol<Fragment>{
    private  static final String CRFL="/r/n";
    @Override
    public Fragment decode(ByteBuffer data, TransportSession session) {

        return null;
    }

    private int indexOf(ByteBuffer data){
//        data.
        return -1;
    }
}
