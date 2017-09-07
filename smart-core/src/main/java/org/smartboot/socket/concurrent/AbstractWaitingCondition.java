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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by jcairns on 12/11/14.
 */
// abstract condition supporting common condition code
abstract class AbstractWaitingCondition implements Condition {


    // keep track of whos waiting so we don't have to synchronize
    // or notify needlessly - when nobody is waiting

    private static final int MAX_WAITERS = 8;

    private static final int WAITER_MASK = MAX_WAITERS-1;

    private static final long WAIT_TIME = PARK_TIMEOUT;

    private final AtomicReferenceArray<Thread> waiter = new AtomicReferenceArray<Thread>(MAX_WAITERS);

    private final AtomicInteger waitCount = new ContendedAtomicInteger(0);
    private final ContendedInt waitCache = new ContendedInt(0);

    /**
     * code below will block until test() returns false
     *
     * @return boolean - true if condition is not satisfied
     */
    @Override
    public abstract boolean test();

    @Override
    public void awaitNanos(long timeout) throws InterruptedException {
        for (;;) {

            try {
                final int waitCount = this.waitCount.get();
                int waitSequence = waitCount;

                if (this.waitCount.compareAndSet(waitCount, waitCount + 1)) {
                    waitCache.value = waitCount+1;

                    long timeNow = System.nanoTime();
                    final long expires = timeNow+timeout;

                    final Thread t = Thread.currentThread();

                    if(waitCount == 0) {
                        // first thread spins

                        int spin = 0;
                        while(test() && expires>timeNow && !t.isInterrupted()) {
                            spin = AbstractCondition.progressiveYield(spin);
                            timeNow = System.nanoTime();
                        }

                        if(t.isInterrupted()) {
                            throw new InterruptedException();
                        }

                        return;
                    } else {
                        // wait to become a waiter
                        int spin = 0;
                        while(test() && !waiter.compareAndSet(waitSequence++ & WAITER_MASK, null, t) && expires>timeNow) {
                            if(spin < Condition.MAX_PROG_YIELD) {
                                spin = AbstractCondition.progressiveYield(spin);
                            } else {
                                LockSupport.parkNanos(MAX_WAITERS*Condition.PARK_TIMEOUT);
                            }

                            timeNow = System.nanoTime();
                        }
                        // are we a waiter?   wait until we are awakened
                        while(test() && (waiter.get((waitSequence-1) & WAITER_MASK) == t) && expires>timeNow && !t.isInterrupted()) {
                            LockSupport.parkNanos((expires-timeNow)>>2);
                            timeNow = System.nanoTime();
                        }

                        if(t.isInterrupted()) {
                            // we are not waiting we are interrupted
                            while(!waiter.compareAndSet((waitSequence-1) & WAITER_MASK, t, null) && waiter.get(0) == t) {
                                LockSupport.parkNanos(PARK_TIMEOUT);
                            }

                            throw new InterruptedException();
                        }

                        return;


                    }
                }
            }finally{
                waitCache.value = waitCount.decrementAndGet();
            }
        }
    }

    @Override
    public void await() throws InterruptedException {
        for(;;) {

            try {
                final int waitCount = this.waitCount.get();
                int waitSequence = waitCount;

                if (this.waitCount.compareAndSet(waitCount, waitCount + 1)) {
                    waitCache.value = waitCount+1;

                    final Thread t = Thread.currentThread();

                    if(waitCount == 0) {
                        int spin = 0;
                        // first thread spinning
                        while(test() && !t.isInterrupted()) {
                            spin = AbstractCondition.progressiveYield(spin);
                        }

                        if(t.isInterrupted()) {
                            throw new InterruptedException();
                        }

                        return;
                    } else {

                        // wait to become a waiter
                        int spin = 0;
                        while(test() && !waiter.compareAndSet(waitSequence++ & WAITER_MASK, null, t) && !t.isInterrupted()) {
                            if(spin < Condition.MAX_PROG_YIELD) {
                                spin = AbstractCondition.progressiveYield(spin);
                            } else {
                                LockSupport.parkNanos(MAX_WAITERS*Condition.PARK_TIMEOUT);
                            }
                        }

                        // are we a waiter?   wait until we are awakened
                        while(test() && (waiter.get((waitSequence-1) & WAITER_MASK) == t) && !t.isInterrupted()) {
                            LockSupport.parkNanos(1_000_000L);
                        }

                        if(t.isInterrupted()) {
                            // we are not waiting we are interrupted
                            while(!waiter.compareAndSet((waitSequence-1) & WAITER_MASK, t, null) && waiter.get(0) == t) {
                                LockSupport.parkNanos(WAIT_TIME);
                            }

                            throw new InterruptedException();
                        }

                        return;

                    }

                }
            } finally {
                waitCache.value = waitCount.decrementAndGet();
            }
        }
    }

    @Override
    public void signal() {
        // only signal if somebody is blocking for it
        if (waitCache.value > 0 || (waitCache.value = waitCount.get()) > 0) {
            int waitSequence = 0;
            for(;;) {
                Thread t;
                while((t = waiter.get(waitSequence++ & WAITER_MASK)) != null) {
                    if(waiter.compareAndSet((waitSequence-1) & WAITER_MASK, t, null)) {
                        LockSupport.unpark(t);
                    } else {
                        LockSupport.parkNanos(WAIT_TIME);
                    }

                    // go through all waiters once, or return if we are finished
                    if(((waitSequence & WAITER_MASK) == WAITER_MASK) || (waitCache.value = waitCount.get()) == 0) {
                        return;
                    }
                }

                // go through all waiters once, or return if we are finished
                if(((waitSequence & WAITER_MASK) == WAITER_MASK) || (waitCache.value = waitCount.get()) == 0) {
                    return;
                }
            }
        }
    }
}
