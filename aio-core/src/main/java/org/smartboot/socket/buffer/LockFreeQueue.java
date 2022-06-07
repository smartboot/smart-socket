package org.smartboot.socket.buffer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/*** 用数组实现无锁有界队列*/
public class LockFreeQueue {
    //代表为空，没有元素
    private static final VirtualBuffer EMPTY = null;
    private final AtomicReferenceArray<VirtualBuffer> atomicReferenceArray;
    //头指针,尾指针
    private final AtomicInteger head = new AtomicInteger(0);
    private final AtomicInteger tail = new AtomicInteger(0);

    public LockFreeQueue(int size) {
        atomicReferenceArray = new AtomicReferenceArray<>(new VirtualBuffer[size]);
    }

    public boolean offer(VirtualBuffer buffer) {
        int index = (tail.get() + 1) % atomicReferenceArray.length();
        if (index == head.get() % atomicReferenceArray.length()) {
            return false;
        }
        if (atomicReferenceArray.compareAndSet(index, EMPTY, buffer)) {
            tail.incrementAndGet();
            return true;
        }
        return offer(buffer);
    }

    public VirtualBuffer poll() {
        if (head.get() == tail.get()) {
            return null;
        }
        int index = (head.get() + 1) % atomicReferenceArray.length();
        VirtualBuffer buffer = atomicReferenceArray.get(index);
        if (buffer == null) {
            //有可能其它线程也在出队
            return poll();
        }
        if (!atomicReferenceArray.compareAndSet(index, buffer, EMPTY)) {
            return poll();
        }
        head.incrementAndGet();
        return buffer;
    }

    public boolean isEmpty() {
        return tail.get() == head.get();
    }

    @Override
    public String toString() {
        return "LockFreeQueue{" +
                ", head=" + head +
                ", tail=" + tail +
                '}';
    }
}
