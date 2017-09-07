package org.smartboot.socket.concurrent;

/**
 * Created by jcairns on 2/18/16.
 */
public abstract class AbstractSpinningCondition implements Condition {

    @Override
    public void awaitNanos(final long timeout) throws InterruptedException {
        long timeNow = System.nanoTime();
        final long expires = timeNow+timeout;

        final Thread t = Thread.currentThread();

        while(test() && expires>timeNow && !t.isInterrupted()) {
            timeNow = System.nanoTime();
        }

        if(t.isInterrupted()) {
            throw new InterruptedException();
        }
    }

    @Override
    public void await() throws InterruptedException {
        final Thread t = Thread.currentThread();

        while(test() && !t.isInterrupted()) {
            ;
        }

        if(t.isInterrupted()) {
            throw new InterruptedException();
        }
    }

    @Override
    public void signal() {

    }
}
