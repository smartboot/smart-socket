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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jcairns on 12/11/14.
 */

// use java sync to signal
abstract class AbstractCondition implements Condition {

    private final ReentrantLock queueLock = new ReentrantLock();

    private final java.util.concurrent.locks.Condition condition = queueLock.newCondition();

    // wake me when the condition is satisfied, or timeout
    @Override
    public void awaitNanos(final long timeout) throws InterruptedException {
        long remaining = timeout;
        queueLock.lock();
        try {
            while(test() && remaining > 0) {
                remaining = condition.awaitNanos(remaining);
            }
        }
        finally {
            queueLock.unlock();
        }
    }

    // wake if signal is called, or wait indefinitely
    @Override
    public void await() throws InterruptedException {
        queueLock.lock();
        try {
            while(test()) {
                condition.await();
            }
        }
        finally {
            queueLock.unlock();
        }
    }

    // tell threads waiting on condition to wake up
    @Override
    public void signal() {
        queueLock.lock();
        try {
            condition.signalAll();
        }
        finally {
            queueLock.unlock();
        }

    }



    /*
     * progressively transition from spin to yield over time
     */
    static int progressiveYield(final int n) {
        if(n > 100) {
            if(n<1000) {
                // "randomly" yield 1:8
                if((n & 0x7) == 0) {
                    LockSupport.parkNanos(PARK_TIMEOUT);
                }
            } else if(n<MAX_PROG_YIELD) {
                // "randomly" yield 1:4
                if((n & 0x3) == 0) {
                    Thread.yield();
                }
            } else {
                Thread.yield();
                return n;
            }
        }
        return n+1;
    }

    /**
     * Wait for timeout on condition
     *
     * @param timeout - the amount of time in units to wait
     * @param unit - the time unit
     * @param condition - condition to wait for
     * @return boolean - true if status was detected
     * @throws InterruptedException - on interrupt
     */
    static boolean waitStatus(final long timeout, final TimeUnit unit, final Condition condition) throws InterruptedException {
        // until condition is signaled

        final long timeoutNanos = TimeUnit.NANOSECONDS.convert(timeout, unit);
        final long expireTime = System.nanoTime() + timeoutNanos;
        // the queue is empty or full wait for something to change
        while (condition.test()) {
            final long now = System.nanoTime();
            if (now > expireTime) {
                return false;
            }

            condition.awaitNanos(expireTime - now - PARK_TIMEOUT);

        }

        return true;
    }


}
