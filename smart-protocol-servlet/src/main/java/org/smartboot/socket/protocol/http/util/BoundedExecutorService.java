package org.smartboot.socket.protocol.http.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Wraps {@link Executor} so that we only ask the wrapped Executor to execute N
 * number of tasks at any given time.
 * 
 * <p>
 * The intention is to use this with {@link ThreadPoolExecutor} with
 * {@link SynchronousQueue} with unbounded max capacity (so that for up to N
 * tasks we keep creating more threads for work, but beyond that we start to
 * push the tasks into the queue of an infinite capacity.)
 * 
 * <p>
 * This is necessary because {@link ThreadPoolExecutor} tries to push work into
 * the queue first and only create more threads once the queue is full, so for a
 * queue with infinite capacity it'll never create threads beyond the core pool
 * size. See http://www.kimchy.org/juc-executorservice-gotcha/ for more
 * discussion of this.
 * 
 * <p>
 * Because there's no call back to tell us when the wrapped
 * {@link ExecutorService} has finished executing something, this class needs to
 * hand out the next task slightly before the wrapped {@link ExecutorService} is
 * done with the previous task. The net result is that the wrapped
 * {@link ExecutorService} will end up running N+1 threads (of which 1 is almost
 * always idle.) I'm not sure how to fix this.
 * 
 * @author Kohsuke Kawaguchi
 */
public class BoundedExecutorService extends AbstractExecutorService {
	/**
	 * The FIFO queue of tasks waiting to be handed to the wrapped
	 * {@link ExecutorService}.
	 */
	private final List<Runnable> tasks = new LinkedList<Runnable>();

	private final ExecutorService base;
	private final int max;

	/**
	 * How many tasks the wrapped {@link ExecutorService} is executing right
	 * now? Touched only in a synchronized block.
	 */
	private int current;

	private boolean isShutdown = false;

	public BoundedExecutorService(ExecutorService base, int max) {
		this.base = base;
		this.max = max;
	}

	public synchronized void execute(final Runnable r) {
		if (isShutdown)
			throw new RejectedExecutionException("already shut down");
		tasks.add(r);
		if (current < max)
			scheduleNext();
	}

	private synchronized void scheduleNext() {
		if (tasks.isEmpty()) {
			if (isShutdown)
				base.shutdown();
			return;
		}
		final Runnable task = tasks.remove(0);
		base.execute(new Runnable() {
			public void run() {
				try {
					task.run();
				} finally {
					done();
				}
			}
		});
		current++;
	}

	private synchronized void done() {
		current--;
		scheduleNext(); // we already know that current<max
	}

	public synchronized void shutdown() {
		isShutdown = true;
		if (tasks.isEmpty())
			base.shutdown();
	}

	public synchronized List<Runnable> shutdownNow() {
		isShutdown = true;
		List<Runnable> r = base.shutdownNow();
		r.addAll(tasks);
		tasks.clear();
		return r;
	}

	public boolean isShutdown() {
		return isShutdown;
	}

	public boolean isTerminated() {
		return base.isTerminated();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return base.awaitTermination(timeout, unit);
	}
}