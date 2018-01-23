package org.smartboot.socket.protocol.http.pool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A simple object pool.
 * 
 * @see http://www.ibm.com/developerworks/java/library/j-jtp07233.html for
 *      ConcurrentHashMap choice.
 * @see http://www.ibm.com/developerworks/java/library/j-jtp09275.html?ca=dgr-
 *      jw22JavaUrbanLegends for choose what put in a pool.
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * @param <T>
 *            Pooled Object.
 */
public class SimplePool<T> implements Pool<T> {

	/** instances currently idle in this pool. */
	protected final BlockingQueue<T> pooled;

	/** active instance (using a ConcurrentHashMap instance as a Set). */
	protected final ConcurrentHashMap<T, Object> borrowed;

	/** Maximum pool size. */
	protected final int capacity;

	/** maximum time (in milliseconds) to peek object from pool. */
	protected final int timeout;

	/** factory instance. */
	protected final ResourceFactory<T> factory;

	/**
	 * Boolean.TRUE if pool instance is in a closing way. all released resource
	 * will be destroyed.
	 */
	protected boolean closing;

	/** minimum idle instance */
	protected final int minIdle;

	/**
	 * Create a new instance of SimplePool with specified parameters.
	 * 
	 * @param factory
	 *            instance of factory to manage resource life cycle.
	 * @param capacity
	 *            Maximum pool size
	 * @param timeout
	 *            acquire time out in milliseconds (<=0 infinite waiting).
	 * @param minIdle
	 *            minimum number of idle resource (Upper limit of minIdle is
	 *            equals to capacity).
	 * @throws IllegalArgumentException
	 *             if capacity <= 0 or if factory is null
	 */
	public SimplePool(final ResourceFactory<T> factory, final int capacity, final int timeout, final int minIdle) {
		super();
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be > 0");
		}
		if (factory == null) {
			throw new IllegalArgumentException("factory cannot be null");
		}
		this.factory = factory;
		this.capacity = capacity;
		this.timeout = timeout;
		this.borrowed = new ConcurrentHashMap<T, Object>(capacity);
		this.pooled = new ArrayBlockingQueue<T>(capacity, Boolean.TRUE);
		this.closing = Boolean.FALSE;
		// create initial idle instance.
		this.minIdle = Math.min(minIdle, capacity);
		if (this.minIdle > 0) {
			for (int i = 0; i < this.minIdle; i++) {
				boolean offered = Boolean.FALSE;
				final T resource = factory.create();
				if (timeout > 0) {
					try {
						offered = pooled.offer(resource, timeout, TimeUnit.MILLISECONDS);
					} catch (final InterruptedException e) {
					}
				} else {
					offered = pooled.offer(resource);
				}
				if (!offered) {
					factory.destroy(resource);
				}
			}
		}
	}

	@Override
	public T acquire() {
		T result = null;
		if (!closing) {
			if (pooled.isEmpty() && borrowed.size() < capacity) {
				// build a new instance
				result = factory.create();
			} else {
				// peek from pool
				try {
					if (timeout > 0) {
						result = pooled.poll(timeout, TimeUnit.MILLISECONDS);
					} else {
						result = pooled.take();
					}
				} catch (final InterruptedException e) {
				}
			}
			if (result != null) {
				borrowed.putIfAbsent(result, Boolean.TRUE);
			}
		}
		return result;
	}

	@Override
	public void release(final T resource) {
		boolean offered = Boolean.FALSE;
		if (resource != null) {
			if (borrowed.remove(resource, Boolean.TRUE)) {
				// if we closing pool, we did not offer in pooled list.
				if (!closing) {
					if (timeout > 0) {
						try {
							offered = pooled.offer(resource, timeout, TimeUnit.MILLISECONDS);
						} catch (final InterruptedException e) {
						}
					} else {
						offered = pooled.offer(resource);
					}
				}
			}
		}
		// discard resource
		if (!offered) {
			factory.destroy(resource);
		}
	}

	@Override
	public void invalidate(final T resource) {
		if (resource != null) {
			borrowed.remove(resource);
			pooled.remove(resource);
			factory.destroy(resource);
		}
	}

	/**
	 * Clears any objects sitting idle in the pool, releasing any associated
	 * resources.
	 */
	public void clear() {
		final List<T> forDiscard = new ArrayList<T>();
		pooled.drainTo(forDiscard);
		for (final T resource : forDiscard) {
			// same as invalidate here
			borrowed.remove(resource);
			pooled.remove(resource);
			factory.destroy(resource);
		}
	}

	/**
	 * Try to drain pool to minimum idle.
	 * 
	 * @return the number of drained resources.
	 */
	public int drain() {
		final int delta = pooled.size() - this.minIdle;
		int drained = 0;
		if (delta > 0) {
			for (int i = 0; i < delta; i++) {
				try {
					final T resource = acquire();
					if (resource != null) {
						drained++;
						invalidate(resource);
					}
				} catch (final Throwable e) {

				}
			}
		}
		return drained;
	}

	/**
	 * Apply the specified process on idle resource.
	 * 
	 * @param function
	 *            the specified process.
	 */
	public void applyOnIdle(final Function<Void, T> function) {
		final Iterator<T> iterator = pooled.iterator();
		while (iterator.hasNext()) {
			function.apply(iterator.next());
		}
	}

	/**
	 * Close the pool. All resource released after this call and all objects
	 * sitting idle will be destroyed.
	 */
	public void close() {
		closing = Boolean.TRUE;
		clear();
	}

	/**
	 * Return the number of instances currently borrowed from this pool
	 * (Shouldn't be called due to performance leak).
	 * 
	 * @return the number of instances currently borrowed from this pool.
	 */
	public int getNumActive() {
		return borrowed.size();
	}

	/**
	 * Return the number of instances currently idle in this pool. This may be
	 * considered an approximation of the number of objects that can be borrowed
	 * without creating any new instances. Returns a negative value if this
	 * information is not available
	 * 
	 * @return the number of instances currently idle in this pool or a negative
	 *         value if unsupported
	 */
	public int getNumIdle() {
		return pooled.size();
	}

	/**
	 * Return the maximum number of instances which can be handle by this pool.
	 * 
	 * @return the maximum number of instances permitted
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * Returns the login timeout value in ms.
	 * 
	 * @return the login timeout value in ms.
	 */
	public int getTimeout() {
		return timeout;
	}

	@Override
	public String toString() {
		return getClass().getName() + " [capacity=" + capacity + ", Num Idle=" + getNumIdle() + "]";
	}

}
