/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: RingBuffer.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @param <T>
 * @author 三刀
 */
final class RingBuffer<T> {

    private static final byte READABLE = 1, READING = 1 << 1, WRITEABLE = 1 << 2, WRITING = 1 << 3;
    /**
     * The queued items
     */
    private final Node<T>[] items;
    /**
     * Main lock guarding all access
     */
    private final ReentrantLock lock;
    /**
     * Condition for waiting takes
     */
    private final Condition notEmpty;
    /**
     * Condition for waiting puts
     */
    private final Condition notFull;

    /*
     * Concurrency control uses the classic two-condition algorithm
     * found in any textbook.
     */
    /**
     * items index for next take, poll, peek or remove
     */
    private int takeIndex;
    /**
     * items index for next put, offer, or add
     */
    private int putIndex;
    /**/
    private boolean needFullSingle = false;
    /**/
    private boolean needEmptySingle = false;
    /**/
    private EventFactory<T> eventFactory;


    /**
     * Creates an {@code ArrayBlockingQueue} with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public RingBuffer(int capacity, EventFactory<T> factory) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.items = new Node[capacity];
        lock = new ReentrantLock(false);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
        this.eventFactory = factory;
    }

    public int nextWriteIndex() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            notFullSignal();
            final Node<T>[] items = this.items;
            Node<T> node = items[putIndex];
            if (node == null) {
                node = new Node<>(eventFactory.newInstance());
                node.status = WRITEABLE;
                items[putIndex] = node;
            }
            while (node.status != WRITEABLE) {
                notFull.await();
                notFullSignal();
                node = items[putIndex];
            }

            node.status = WRITING;
            int index = putIndex;
            if (++putIndex == items.length) {
                putIndex = 0;
            }
            return index;
        } finally {
            lock.unlock();
        }
    }

    public int tryNextWriteIndex() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            notFullSignal();
            final Node<T>[] items = this.items;
            Node<T> node = items[putIndex];
            if (node == null) {
                node = new Node<>(eventFactory.newInstance());
                node.status = WRITEABLE;
                items[putIndex] = node;
            }
            if (node.status != WRITEABLE) {
                return -1;
            }

            node.status = WRITING;
            int index = putIndex;
            if (++putIndex == items.length) {
                putIndex = 0;
            }
            return index;
        } finally {
            lock.unlock();
        }
    }

    public void publishWriteIndex(int sequence) {
        Node<T> node = items[sequence];
        if (node.status != WRITING) {
            throw new RuntimeException("invalid status");
        }
        node.status = READABLE;
        final ReentrantLock lock = this.lock;
        needEmptySingle = true;
        if (lock.tryLock()) {
            try {
                notFullSignal();
            } finally {
                lock.unlock();
            }
        }
    }

    public T get(int sequence) {
        return items[sequence].entity;
    }

    public int tryNextReadIndex() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            notFullSignal();
            final Node[] items = this.items;
            Node x = items[takeIndex];
            if (x == null || x.status != READABLE) {
                return -1;
            }
            x.status = READING;
            int index = takeIndex;
            if (++takeIndex == items.length) {
                takeIndex = 0;
            }
            return index;
        } finally {
            lock.unlock();
        }
    }

    public int nextReadIndex() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            notFullSignal();
            final Node[] items = this.items;
            Node x = items[takeIndex];
            while (x == null || x.status != READABLE) {
                notEmpty.await();
                notFullSignal();
                x = items[takeIndex];
            }
            x.status = READING;
            int index = takeIndex;
            if (++takeIndex == items.length) {
                takeIndex = 0;
            }
            return index;
        } finally {
            lock.unlock();
        }
    }

    private void notFullSignal() {
        if (needFullSingle) {
            notFull.signal();
            needFullSingle = false;
        }
        if (needEmptySingle) {
            notEmpty.signal();
            needEmptySingle = false;
        }
    }

    public void publishReadIndex(int sequence) {
        Node<T> node = items[sequence];
        if (node.status != READING) {
            throw new RuntimeException("invalid status");
        }
        eventFactory.restEntity(node.entity);
        node.status = WRITEABLE;
        final ReentrantLock lock = this.lock;
        needFullSingle = true;
        if (lock.tryLock()) {
            try {
                notFullSignal();
            } finally {
                lock.unlock();
            }
        }
    }


    class Node<T1> {

        private byte status;
        private T1 entity;

        Node(T1 entity) {
            this.entity = entity;
        }
    }
}
