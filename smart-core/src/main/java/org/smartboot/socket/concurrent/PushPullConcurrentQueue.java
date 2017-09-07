package org.smartboot.socket.concurrent;

/*
 * #%L
 * Conversant Disruptor
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tuned version of Martin Thompson's push pull queue
 *
 * Transfers from a single thread writer to a single thread reader are orders of nanoseconds (3-5)
 *
 * This code is optimized and tested using a 64bit HotSpot JVM on an Intel x86-64 environment.  Other
 * environments should be carefully tested before using in production.
 *
 * Created by jcairns on 5/28/14.
 */
public class PushPullConcurrentQueue<E> implements ConcurrentQueue<E> {
    protected final int size;
    protected final long mask;

    protected final AtomicLong tail = new ContendedAtomicLong(0L);
    protected final ContendedLong tailCache = new ContendedLong();

    protected final E[] buffer;

    protected final AtomicLong head = new ContendedAtomicLong(0L);
    protected final ContendedLong headCache = new ContendedLong();

    public PushPullConcurrentQueue(final int size) {
        int rs = 1;
        while(rs < size) rs <<= 1;
        this.size = rs;
        this.mask = rs-1;

        buffer = (E[])new Object[this.size];
    }


    @Override
    public boolean offer(final E e) {
        if(e != null) {
            final long tail = this.tail.get();
            final long queueStart = tail - size;
            if((headCache.value > queueStart) || ((headCache.value = head.get()) > queueStart)) {
                final int dx = (int) (tail & mask);
                buffer[dx] = e;
                this.tail.set(tail+1L);
                return true;
            } else {
                return false;
            }
        } else {
            throw new NullPointerException("Invalid element");
        }
    }

    @Override
    public E poll() {
        final long head = this.head.get();
        if((head < tailCache.value) || (head < (tailCache.value = tail.get()))) {
            final int dx = (int)(head & mask);
            final E e = buffer[dx];
            buffer[dx] = null;

            this.head.set(head+1L);
            return e;
        } else {
            return null;
        }
    }

    @Override
    public int remove(final E[] e) {
        int n = 0;

        headCache.value = this.head.get();

        final int nMax = e.length;
        for(long i = headCache.value, end = tail.get(); n<nMax && i<end; i++) {
            final int dx = (int) (i & mask);
            e[n++] = buffer[dx];
            buffer[dx] = null;
        }

        this.head.set(headCache.value+n);

        return n;
    }

    @Override
    public void clear() {
        for(int i=0; i<buffer.length; i++) {
            buffer[i] = null;
        }
        head.set(tail.get());
    }


    @Override
    public final E peek() {
        return buffer[(int)(head.get() & mask)];
    }

    /**
     * This implemention is known to be broken if preemption were to occur after
     * reading the tail pointer.
     *
     * Code should not depend on size for a correct result.
     *
     * @return int - possibly the size, or possibly any value less than capacity()
     */
    @Override
    public final int size() {
        return (int)Math.max(tail.get() - head.get(), 0);
    }

    @Override
    public int capacity() {
        return size;
    }

    @Override
    public final boolean isEmpty() {
        return tail.get() == head.get();
    }

    @Override
    public final boolean contains(Object o) {
        if(o != null) {
            for(long i = head.get(), end = tail.get(); i<end; i++) {
                final E e = buffer[(int)(i & mask)];
                if(o.equals(e)) {
                    return true;
                }
            }
        }
        return false;
    }
}
