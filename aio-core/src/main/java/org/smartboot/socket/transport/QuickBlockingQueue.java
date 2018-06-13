package org.smartboot.socket.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

final class QuickBlockingQueue {


    private final ByteBuffer[] items;

    private final ReentrantLock lock = new ReentrantLock(false);


    private final Condition notEmpty = lock.newCondition();

    private final Condition notFull = lock.newCondition();

    int takeIndex;

    int putIndex;

    int count;

    public QuickBlockingQueue(int capacity) {
        this.items = new ByteBuffer[capacity];
    }

    private void enqueue(ByteBuffer x) {
        items[putIndex] = x;
        if (++putIndex == items.length) {
            putIndex = 0;
        }
        count++;
        notEmpty.signal();
    }


    public BufferArray expectRemaining(int maxSize) {
        lock.lock();
        try {
            if (count == 1) {
                ByteBuffer x = items[takeIndex];
                items[takeIndex] = null;
                if (++takeIndex == items.length) {
                    takeIndex = 0;
                }
                count--;
                notFull.signal();
                return new BufferArray(x.remaining(), x);
            }
            if (count == 0) {
                throw new RuntimeException("aa");
            }
            int findIndex = this.takeIndex;
            int remain = items[findIndex].remaining();

            int bufferLength = 1;
            while (remain <= maxSize && bufferLength < count) {
                if (++findIndex == items.length) {
                    findIndex = 0;
                }
                remain += items[findIndex].remaining();
                bufferLength++;
            }
            if (remain > maxSize && bufferLength > 1) {
                bufferLength -= 1;
                findIndex -= 1;
                if (findIndex < 0) {
                    findIndex = items.length - 1;
                }
                remain -= items[findIndex].remaining();
            }
            ByteBuffer[] buffers = new ByteBuffer[bufferLength];
            for (int i = 0; i < bufferLength; i++) {
                buffers[i] = items[takeIndex];
                items[takeIndex] = null;
                if (++takeIndex == items.length) {
                    takeIndex = 0;
                }
            }
            count -= bufferLength;
            notFull.signal();
            return new BufferArray(remain, buffers);
        } finally {
            lock.unlock();
        }
    }


    public void put(ByteBuffer e) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                notFull.await();
            }
            enqueue(e);
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

class BufferArray {
    private final long totalLength;
    private ByteBuffer[] buffers;
    private long remainLength;

    public BufferArray(final int totalLength, ByteBuffer... buffers) {
        this.buffers = buffers;
        this.totalLength = totalLength;
        this.remainLength = totalLength;
    }

    public ByteBuffer[] getBuffers() {
        return buffers;
    }

    public void setBuffers(ByteBuffer[] buffers) {
        this.buffers = buffers;
    }

    public long getRemainLength() {
        return remainLength;
    }

    public void setRemainLength(long remainLength) {
        this.remainLength = remainLength;
    }

    public long writeSize(long size) {
        return remainLength -= size;
    }

    public long getTotalLength() {
        return totalLength;
    }
}