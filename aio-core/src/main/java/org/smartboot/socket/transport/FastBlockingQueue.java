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

    public int expectRemaining(int maxSize) {
        lock.lock();
        try {
            if (remaining <= maxSize || count == 1) {
                return remaining;
            }
            final ByteBuffer[] items = this.items;
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


    public void put(ByteBuffer e) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            final ByteBuffer[] items = this.items;
            while (count == items.length) {
                notFull.await();
            }

            items[putIndex] = e;
            if (++putIndex == items.length)
                putIndex = 0;
            count++;
            remaining += e.remaining();
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public ByteBuffer poll() {
        lock.lock();
        try {
            if (count == 0) {
                return null;
            }
            final ByteBuffer[] items = this.items;
            ByteBuffer x = items[takeIndex];
            items[takeIndex] = null;
            if (++takeIndex == items.length)
                takeIndex = 0;
            count--;
            remaining -= x.remaining();
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }

    public void pollInto(ByteBuffer destBuffer) {
        lock.lock();
        try {
            final ByteBuffer[] items = this.items;
            int takeIndex = this.takeIndex;
            int count = this.count;
            int remaining = this.remaining;
            while (destBuffer.hasRemaining()) {
                ByteBuffer x = items[takeIndex];
                destBuffer.put(x);
                items[takeIndex] = null;
                if (++takeIndex == items.length)
                    takeIndex = 0;
                count--;
                remaining -= x.remaining();
            }
            this.takeIndex = takeIndex;
            this.count = count;
            this.remaining = remaining;
            notFull.signal();
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