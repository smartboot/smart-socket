package org.smartboot.socket.transport;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import org.smartboot.socket.transport.disruptor.ReadEvent;

/**
 * @author Seer
 * @version V1.0 , 2017/9/18
 */
public class ReadEventHandler implements WorkHandler<ReadEvent> {
//    @Override
//    public void onEvent(ReadEvent o, long l, boolean b) throws Exception {
//        o.getAioSession().readFromChannel();
//    }

    @Override
    public void onEvent(ReadEvent readEvent) throws Exception {
        readEvent.getAioSession().readFromChannel();
    }
}
