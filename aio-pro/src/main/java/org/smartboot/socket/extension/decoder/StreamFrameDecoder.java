/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StreamFrameDecoder.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.extension.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 流式解码器(未严格测试)
 *
 * @author 三刀
 * @version V1.0 , 2017/10/21
 */
public class StreamFrameDecoder {
    private BinaryBuffer buffer = new BinaryBuffer(1024);

    public StreamFrameDecoder(int length) {
        buffer.contentLength = length;
    }

    public boolean decode(ByteBuffer byteBuffer) {
        if (buffer.binWriteLength == buffer.contentLength) {
            return false;
        }
        while (byteBuffer.hasRemaining()) {
            try {
                buffer.put(byteBuffer.get());
            } catch (InterruptedException e) {
                throw new RuntimeException("invalid content length");
            }
            buffer.binWriteLength++;
            if (buffer.binWriteLength == buffer.contentLength) {
                return true;
            }
        }
        return false;
    }

    public InputStream getInputStream() {
        return buffer;
    }

    private class BinaryBuffer extends InputStream
            implements java.io.Serializable {

        /**
         * Serialization ID. This class relies on default serialization
         * even for the items array, which is default-serialized, even if
         * it is empty. Otherwise it could not be declared final, which is
         * necessary here.
         */
        private static final long serialVersionUID = -817911632652898426L;
        /**
         * The queued items
         */
        final byte[] items;
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
        public int binWriteLength = 0;
        public int binReadLength = 0;
        /**
         * items index for next take, poll, peek or remove
         */
        int takeIndex;

        /*
         * Concurrency control uses the classic two-condition algorithm
         * found in any textbook.
         */
        /**
         * items index for next put, offer, or add
         */
        int putIndex;
        /**
         * Number of elements in the queue
         */
        int count;
        private int contentLength;

        /**
         * Creates an {@code ArrayBlockingQueue} with the given (fixed)
         * capacity and default access policy.
         *
         * @param capacity the capacity of this queue
         * @throws IllegalArgumentException if {@code capacity < 1}
         */
        public BinaryBuffer(int capacity) {
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
        public BinaryBuffer(int capacity, boolean fair) {
            if (capacity <= 0)
                throw new IllegalArgumentException();
            this.items = new byte[capacity];
            lock = new ReentrantLock(fair);
            notEmpty = lock.newCondition();
            notFull = lock.newCondition();
        }

        @Override
        public int read() throws IOException {
            if (binReadLength == contentLength) {
                return -1;
            }
            try {
                return take();
            } catch (InterruptedException e) {
                throw new IOException(e);
            } finally {
                binReadLength++;
            }
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            if (binReadLength >= contentLength) {
                return -1;
            }

            int i = 0;
            try {
                for (; i < len; i++) {
                    if (binReadLength >= contentLength) {
                        break;
                    }
                    int c = read();
                    b[off + i] = (byte) c;
                }
            } catch (IOException ee) {
            }
            return i;
        }

        @Override
        public int available() throws IOException {
            return binWriteLength < contentLength ? size() : 0;
        }

        /**
         * Throws NullPointerException if argument is null.
         *
         * @param v the element
         */
        private void checkNotNull(Object v) {
            if (v == null)
                throw new NullPointerException();
        }

        /**
         * Inserts element at current put position, advances, and signals.
         * Call only when holding lock.
         */
        private void enqueue(byte x) {
            // assert lock.getHoldCount() == 1;
            // assert items[putIndex] == null;
            final byte[] items = this.items;
            items[putIndex] = x;
            if (++putIndex == items.length)
                putIndex = 0;
            count++;
            notEmpty.signal();
        }

        /**
         * Extracts element at current take position, advances, and signals.
         * Call only when holding lock.
         */
        private byte dequeue() {
            // assert lock.getHoldCount() == 1;
            // assert items[takeIndex] != null;
            final byte[] items = this.items;
            @SuppressWarnings("unchecked")
            byte x = items[takeIndex];
//        items[takeIndex] = null;
            if (++takeIndex == items.length)
                takeIndex = 0;
            count--;
            notFull.signal();
            return x;
        }

        /**
         * Inserts the specified element at the tail of this queue, waiting
         * for space to become available if the queue is full.
         *
         * @throws InterruptedException {@inheritDoc}
         * @throws NullPointerException {@inheritDoc}
         */
        public void put(byte e) throws InterruptedException {
            checkNotNull(e);
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            try {
                while (count == items.length)
                    notFull.await();
                enqueue(e);
            } finally {
                lock.unlock();
            }
        }

        public byte take() throws InterruptedException {
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            try {
                while (count == 0)
                    notEmpty.await();
                return dequeue();
            } finally {
                lock.unlock();
            }
        }


        // this doc comment is overridden to remove the reference to collections
        // greater in size than Integer.MAX_VALUE

        /**
         * Returns the number of elements in this queue.
         *
         * @return the number of elements in this queue
         */
        public int size() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }

    }
}
