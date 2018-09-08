package org.smartboot.socket.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

final class FastBlockingQueue {


    private final ByteBuffer[] items;

    private final ReentrantLock lock = new ReentrantLock(false);


    private final Condition notEmpty = lock.newCondition();

    private final Condition notFull = lock.newCondition();

    int takeIndex;

    int putIndex;

    int count;

    int remaining;

    public FastBlockingQueue(int capacity) {
        this.items = new ByteBuffer[capacity];
    }

    private void enqueue(ByteBuffer x) {
        items[putIndex] = x;
        if (++putIndex == items.length) {
            putIndex = 0;
        }
        count++;
        remaining += x.remaining();
        notEmpty.signal();
    }


    private ByteBuffer dequeue() {
        ByteBuffer x = items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) {
            takeIndex = 0;
        }
        count--;
        remaining -= x.remaining();
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
            int remain = items[takeIndex].remaining();
            while (remain <= maxSize) {
                remain += (preCount = items[++takeIndex % items.length].remaining());
            }
            return remain - preCount;
        } finally {
            lock.unlock();
        }
    }


    public int put(ByteBuffer e) throws InterruptedException {
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

    public ByteBuffer poll() {
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    public void pollInto(ByteBuffer destBuffer) {
        lock.lock();
        try {
            while (destBuffer.hasRemaining()) {
                destBuffer.put(dequeue());
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