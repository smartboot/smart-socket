/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: FutureCompletionHandler.java
 * Date: 2021-07-29
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class FutureCompletionHandler<V, A> implements CompletionHandler<V, A>, Future<V> {
    private V result;
    private boolean done = false;
    private boolean cancel = false;
    private Throwable exception;

    @Override
    public void completed(V result, A selectionKey) {
        this.result = result;
        done = true;
        synchronized (this) {
            this.notify();
        }
    }

    @Override
    public void failed(Throwable exc, A attachment) {
        exception = exc;
        done = true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (done || cancel) {
            return false;
        }
        cancel = true;
        done = true;
        synchronized (this) {
            notify();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public synchronized V get() throws InterruptedException, ExecutionException {
        if (done) {
            if (exception != null) {
                throw new ExecutionException(exception);
            }
            return result;
        } else {
            wait();
        }
        return get();
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (done) {
            return get();
        } else {
            wait(unit.toMillis(timeout));
        }
        if (done) {
            return get();
        }
        throw new TimeoutException();
    }

}
