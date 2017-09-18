package org.smartboot.socket.transport.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * @author Seer
 * @version V1.0 , 2017/9/18
 */
public class ReadEventFactory implements EventFactory<ReadEvent>{
    @Override
    public ReadEvent newInstance() {
        return new ReadEvent();
    }
}
