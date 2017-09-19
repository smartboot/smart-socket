package org.smartboot.socket.transport;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WorkProcessor;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.service.SmartFilter;
import org.smartboot.socket.transport.AioSession.Attachment;
import org.smartboot.socket.transport.disruptor.ReadEvent;
import org.smartboot.socket.transport.disruptor.ReadEventFactory;
import org.smartboot.socket.transport.disruptor.ReadEventProducer;
import org.smartboot.socket.util.StateMachineEnum;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 读写事件回调处理类
 */
class AioCompletionHandler implements CompletionHandler<Integer, Attachment> {
    private static final Logger LOGGER = LogManager.getLogger(AioCompletionHandler.class);
    private boolean asyncRead;
    ReadEventProducer producer;

    public AioCompletionHandler(boolean asyncRead) {
        this.asyncRead = asyncRead;
        if (asyncRead) {
            ReadEventFactory factory = new ReadEventFactory();
            int bufferSize = 2048;
            Executor threadPool = Executors.newCachedThreadPool();


            RingBuffer<ReadEvent> ringBuffer = RingBuffer.create(ProducerType.MULTI, factory, bufferSize, new BlockingWaitStrategy());
            SequenceBarrier barriers = ringBuffer.newBarrier();
            Sequence consumerSequence = new Sequence(-1);
            ReadEventHandler eventHandler = new ReadEventHandler();
            threadPool.execute(new WorkProcessor<>(ringBuffer, barriers, eventHandler, new IgnoreExceptionHandler(), consumerSequence));
            threadPool.execute(new WorkProcessor<>(ringBuffer, barriers, eventHandler, new IgnoreExceptionHandler(), consumerSequence));
            threadPool.execute(new WorkProcessor<>(ringBuffer, barriers, eventHandler, new IgnoreExceptionHandler(), consumerSequence));
            threadPool.execute(new WorkProcessor<>(ringBuffer, barriers, eventHandler, new IgnoreExceptionHandler(), consumerSequence));
            producer = new ReadEventProducer(ringBuffer);

        }
    }

    @Override
    public void completed(final Integer result, final Attachment attachment) {
        //出现result为0,说明代码存在问题
        if (result == 0) {
            LOGGER.error("result is 0");
        }
        //读操作回调
        if (attachment.isRead()) {
            if (result == -1) {
                attachment.getAioSession().getIoServerConfig().getProcessor().stateEvent(attachment.getAioSession(), StateMachineEnum.INPUT_SHUTDOWN, null);
                return;
            }
            // 接收到的消息进行预处理
            SmartFilter[] filters = attachment.getAioSession().getIoServerConfig().getFilters();
            if (filters != null) {
                for (SmartFilter h : filters) {
                    h.readFilter(attachment.getAioSession(), result);
                }
            }

            if (asyncRead) {
                producer.onData(attachment.getAioSession());
            } else {
                attachment.getAioSession().readFromChannel();
            }
        } else {
            attachment.getAioSession().writeToChannel();
        }

    }

    @Override
    public void failed(Throwable exc, Attachment attachment) {
        attachment.getAioSession().getIoServerConfig().getProcessor().stateEvent(attachment.getAioSession(), attachment.isRead() ? StateMachineEnum.INPUT_EXCEPTION : StateMachineEnum.OUTPUT_EXCEPTION, exc);
    }
}