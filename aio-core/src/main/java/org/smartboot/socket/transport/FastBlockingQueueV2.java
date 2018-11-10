package org.smartboot.socket.transport;

import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

final class FastBlockingQueueV2 {


    private final ByteBuf[] items;

    private final ReentrantLock lock = new ReentrantLock(false);


    private final Condition notEmpty = lock.newCondition();

    private final Condition notFull = lock.newCondition();

    int takeIndex;

    int putIndex;

    int count;

    int remaining;

    public FastBlockingQueueV2(int capacity) {
        this.items = new ByteBuf[capacity];
    }

    private void enqueue(ByteBuf x) {
        items[putIndex] = x;
        if (++putIndex == items.length) {
            putIndex = 0;
        }
        count++;
        remaining += x.buffer().remaining();
        notEmpty.signal();
    }


    private ByteBuf dequeue() {
        ByteBuf x = items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) {
            takeIndex = 0;
        }
        count--;
        remaining -= x.buffer().remaining();
        notFull.signal();
        return x;
    }

    public int expectRemaining(int maxSize) {
        lock.lock();
        try {
            if (remaining <= maxSize || count == 1) {
                return remaining;
            }

            int takeIndex = this.takeIndex;
            int preCount = 0;
            int remain = items[takeIndex].buffer().remaining();
            while (remain <= maxSize) {
                remain += (preCount = items[++takeIndex % items.length].buffer().remaining());
            }
            return remain - preCount;
        } finally {
            lock.unlock();
        }
    }


    public int put(ByteBuf e) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                notFull.await();
            }
            enqueue(e);
            return count;
        } finally {
            lock.unlock();
        }
    }

    public ByteBuf poll() {
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    public void pollInto(ByteBuffer destBuffer, BufferPage page) {
        lock.lock();
        try {
            while (destBuffer.hasRemaining()) {
                ByteBuf byteBuf=dequeue();
                destBuffer.put(byteBuf.buffer());
                byteBuf.release();
            }
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}