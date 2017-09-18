package org.smartboot.socket.transport;

import com.lmax.disruptor.EventHandler;
import org.smartboot.socket.transport.disruptor.ReadEvent;

/**
 * @author Seer
 * @version V1.0 , 2017/9/18
 */
public class ReadEventHandler implements EventHandler<ReadEvent> {
    @Override
    public void onEvent(ReadEvent o, long l, boolean b) throws Exception {
        o.getAioSession().readFromChannel();
    }
}
