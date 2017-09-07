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

/*
 * Return true once a condition is satisfied
 * Created by jcairns on 12/11/14.
 */
interface Condition {

    long PARK_TIMEOUT = 50L;

    int MAX_PROG_YIELD = 2000;

    // return true if the queue condition is satisfied
    boolean test();

    // wake me when the condition is satisfied, or timeout
    void awaitNanos(final long timeout) throws InterruptedException;

    // wake if signal is called, or wait indefinitely
    void await() throws InterruptedException;

    // tell threads waiting on condition to wake up
    void signal();
}
