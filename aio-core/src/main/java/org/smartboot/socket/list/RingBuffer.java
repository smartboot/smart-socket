/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.smartboot.socket.list;

import org.smartboot.socket.transport.AioSession;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RingBuffer {

    /**
     * The queued items
     */
    final Node[] items;
    /**
     * Main lock guarding all access
     */
    final ReentrantLock lock;
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
    int takeIndex;
    /**
     * items index for next put, offer, or add
     */
    int putIndex;
    private boolean needSingle = false;


    /**
     * Creates an {@code ArrayBlockingQueue} with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public RingBuffer(int capacity) {
        this(capacity, false);
    }

    /**
     * Creates an {@code ArrayBlockingQueue} with the given (fixed)
     * capacity and the specified access policy.
     *
     * @param capacity the capacity of this queue
     * @param fair     if {@code true} then queue accesses for threads blocked
     *                 on insertion or removal, are processed in FIFO order;
     *                 if {@code false} the access order is unspecified.
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public RingBuffer(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = new Node[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting
     * for space to become available if the queue is full.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(AioSession session, int size) throws InterruptedException {
//        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            notFullSignal();
            final Node[] items = this.items;
            Node node = items[putIndex];
            if (node == null) {
                node = new Node();
                node.status = NodeStatus.WRITEABLE;
                items[putIndex] = node;
            }
            while (node.status != NodeStatus.WRITEABLE) {
                notFull.await();
                node = items[putIndex];
            }

            node.session = session;
            node.size = size;
            node.status = NodeStatus.READABLE;
            if (++putIndex == items.length)
                putIndex = 0;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public Node poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            notFullSignal();
            final Node[] items = this.items;
            Node x = items[takeIndex];
            if (x == null || x.status != NodeStatus.READABLE) {
                return null;
            }

            if (++takeIndex == items.length)
                takeIndex = 0;
            x.status = NodeStatus.READING;
            return x;
        } finally {
            lock.unlock();
        }
    }

    public Node take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            notFullSignal();
            final Node[] items = this.items;
            Node x = items[takeIndex];
            while (x == null || x.status != NodeStatus.READABLE) {
                notEmpty.await();
                notFullSignal();
                x = items[takeIndex];
            }

            if (++takeIndex == items.length)
                takeIndex = 0;
            x.status = NodeStatus.READING;
            return x;
        } finally {
            lock.unlock();
        }
    }

    private void notFullSignal() {
        if (needSingle) {
            notFull.signal();
            needSingle = false;
        }
    }

    public void resetNode(Node node) {
        if (node.status != NodeStatus.READING) {
            throw new RuntimeException("invalid status");
        }
        node.session = null;
        node.size = -1;
        node.status = NodeStatus.WRITEABLE;
        final ReentrantLock lock = this.lock;
        needSingle = true;
        if (lock.tryLock()) {
            try {
                notFullSignal();
            } finally {
                lock.unlock();
            }
        }
    }


}
