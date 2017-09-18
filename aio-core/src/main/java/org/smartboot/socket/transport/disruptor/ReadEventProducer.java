package org.smartboot.socket.transport.disruptor;

import com.lmax.disruptor.RingBuffer;
import org.smartboot.socket.transport.AioSession;

/**
 * @author Seer
 * @version V1.0 , 2017/9/18
 */
public class ReadEventProducer {
    private final RingBuffer<ReadEvent> ringBuffer;

    public ReadEventProducer(RingBuffer<ReadEvent> ringBuffer)
    {
        this.ringBuffer = ringBuffer;
    }

    public void onData(AioSession bb)
    {
        long sequence = ringBuffer.next();  // Grab the next sequence
        try
        {
            ReadEvent event = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence
            event.setAioSession(bb);  // Fill with data
        }
        finally
        {
            ringBuffer.publish(sequence);
        }
    }
}
