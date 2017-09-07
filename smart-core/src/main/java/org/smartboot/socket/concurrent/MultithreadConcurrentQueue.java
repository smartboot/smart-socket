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
 * This is the disruptor implemented for multiple simultaneous reader and writer threads.
 *
 * This data structure approaches 20-40ns for transfers on fast hardware.
 *
 * This code is optimized and tested using a 64bit HotSpot JVM on an Intel x86-64 environment.  Other
 * environments should be carefully tested before using in production.
 *
 *
 * Created by jcairns on 5/29/14.
 */
public class MultithreadConcurrentQueue<E> implements ConcurrentQueue<E> {
    /*
     * Note to future developers/maintainers - This code is highly tuned
     * and possibly non-intuitive.    Rigorous performance and functional
     * testing should accompany any proposed change
     *
     */

    // maximum allowed capacity
    // this must always be a power of 2
    //
    protected final int      size;
    // we need to compute a position in the ring buffer
    // modulo size, since size is a power of two
    // compute the bucket position with x&(size-1)
    // aka x&mask
    protected final long     mask;

    // notes about modulos size:
    // for positive x, x % size = x & (size-1)
    // for negative values, x % size = -(-x&(size-1))
    // this is because the & takes off the sign
    // we don't need to compute the precise modulo
    // because the bitwise AND still captures the
    // correct sequence even for negative values,
    // i.e. 0, 1, 2, ..., m, 0, 1, 2, ..., m


    // this data structure is sensitive to the head/tail sequence numbers
    // rolling negative, the capacity check comparisons have all been cast
    // with positive sequence numbers in mind

    // now that we are using longs, good luck living long enough
    // to see this roll in production ;-)

    // ...in case your wondering the answer is 292 years

    // The readers must know if the tail has been updated
    // by concurrently executing write operations.
    // This is updated at the end of the write operation
    // to allow readers thread safe access to the queue tail.


    // the sequence number of the end of the queue
    protected final AtomicLong tail = new ContendedAtomicLong(0L);
    // use the value in the L1 cache rather than reading from memory when possible
    protected final ContendedLong tailCache = new ContendedLong(0L);
    protected final AtomicLong tailCursor = new ContendedAtomicLong(0L);

    // a ring buffer representing the queue
    protected final E[] buffer;

    // the sequence number of the start of the queue
    protected final AtomicLong head =  new ContendedAtomicLong(0L);
    protected final ContendedLong headCache = new ContendedLong(0L);
    protected final AtomicLong headCursor = new ContendedAtomicLong(0L);

    /**
     * Construct a blocking queue of the given fixed capacity.
     *
     * Note: actual capacity will be the next power of two
     * larger than capacity.
     *
     * @param capacity maximum capacity of this queue
     */

    public MultithreadConcurrentQueue(final int capacity) {
        int c = 1;
        while(c < capacity) c <<=1;
        size = c;
        mask = size - 1L;
        buffer = (E[])new Object[size];
    }

    @Override
    public boolean offer(E e) {
        int spin = 0;

        for(;;) {
            final long tailSeq = tail.get();
            // never offer onto the slot that is currently being polled off
            final long queueStart = tailSeq - size;

            // will this sequence exceed the capacity
            if((headCache.value > queueStart) || ((headCache.value = head.get()) > queueStart)) {
                final long tailNext = tailSeq + 1L;
                // does the sequence still have the expected
                // value
                if(tailCursor.compareAndSet(tailSeq, tailNext)) {

                    try {
                        // tailSeq is valid
                        // and we got access without contention

                        // convert sequence number to slot id
                        final int tailSlot = (int)(tailSeq&mask);
                        buffer[tailSlot] = e;

                        return true;
                    } finally {
                        tail.set(tailNext);
                    }
                } // else - sequence misfire, somebody got our spot, try again
            } else {
                // exceeded capacity
                return false;
            }

            spin = AbstractCondition.progressiveYield(spin);
        }
    }

    @Override
    public E poll() {
        int spin = 0;

        for(;;) {
            final long head = this.head.get();
            // is there data for us to poll
            if((tailCache.value > head) || (tailCache.value= tail.get()) > head) {
                final long headNext = head+1L;
                // check if we can update the sequence
                if(headCursor.compareAndSet(head, headNext)) {
                    try {
                        // copy the data out of slot
                        final int pollSlot = (int)(head&mask);
                        final E   pollObj  = (E) buffer[pollSlot];

                        // got it, safe to read and free
                        buffer[pollSlot] = null;

                        return pollObj;
                    } finally {
                        this.head.set(headNext);
                    }
                } // else - somebody else is reading this spot already: retry
            } else {
                return null;
                // do not notify - additional capacity is not yet available
            }

            // this is the spin waiting for access to the queue
            spin = AbstractCondition.progressiveYield(spin);
        }
    }

    @Override
    public final E peek() {
        return buffer[(int)(head.get()&mask)];
    }


    @Override
    // drain the whole queue at once
    public int remove(final E[] e) {

        /* This employs a "batch" mechanism to load all objects from the ring
         * in a single update.    This could have significant cost savings in comparison
         * with poll
         */
        final int maxElements = e.length;
        int spin = 0;

        for(;;) {
            final long pollPos = head.get(); // prepare to qualify?
            // is there data for us to poll
            // note we must take a difference in values here to guard against
            // integer overflow
            final int nToRead = Math.min((int)(tail.get() - pollPos), maxElements);
            if(nToRead > 0 ) {

                for(int i=0; i<nToRead;i++) {
                    final int pollSlot = (int)((pollPos+i)&mask);
                    e[i] = buffer[pollSlot];
                }

                // if we still control the sequence, update and return
                if(headCursor.compareAndSet(pollPos,  pollPos+nToRead)) {
                    head.set(pollPos+nToRead);
                    return nToRead;
                }
            } else {
                // nothing to read now
                return 0;
            }
            // wait for access
            spin = AbstractCondition.progressiveYield(spin);
        }
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
        // size of the ring
        // note these values can roll from positive to
        // negative, this is properly handled since
        // it is a difference
        return (int)Math.max((tail.get() - head.get()), 0);
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
    public void clear() {
        int spin = 0;
        for(;;) {
            final long head = this.head.get();
            if(headCursor.compareAndSet(head, head+1)) {
                for(;;) {
                    final long tail = this.tail.get();
                    if (tailCursor.compareAndSet(tail, tail + 1)) {

                        // we just blocked all changes to the queue

                        // remove leaked refs
                        for (int i = 0; i < buffer.length; i++) {
                            buffer[i] = null;
                        }

                        // advance head to same location as current end
                        this.head.set(tail+1);
                        this.tail.set(tail+1);
                        headCursor.set(tail + 1);

                        return;
                    }
                    spin = AbstractCondition.progressiveYield(spin);
                }
            }
            spin = AbstractCondition.progressiveYield(spin);
        }
    }

    @Override
    public final boolean contains(Object o) {
        for(int i=0; i<size(); i++) {
            final int slot = (int)((head.get() + i) & mask);
            if(buffer[slot]!= null && buffer[slot].equals(o)) return true;
        }
        return false;
    }

}
