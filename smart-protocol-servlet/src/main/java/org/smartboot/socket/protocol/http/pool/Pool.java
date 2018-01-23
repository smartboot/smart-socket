package org.smartboot.socket.protocol.http.pool;

/**
 * A Pool interface based on {@link http
 * ://en.wikipedia.org/wiki/Object_pool_pattern }.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 * @param <T>
 *            Pooled Object.
 */
public interface Pool<T> {

	/**
	 * Acquire a resource from pool.
	 * 
	 * @return a resource T or null if no resource could be acquire.
	 */
	public abstract T acquire();

	/**
	 * Return specified resource in pool.
	 * 
	 * @param resource
	 *            the specified resource.
	 */
	public abstract void release(final T resource);

	/**
	 * Invalidate specified resource and remove it from pool. This method should
	 * be used when an object that has been borrowed is determined (due to an
	 * exception or other problem) to be invalid.
	 * 
	 * @param resource
	 *            the specified resource.
	 */
	public abstract void invalidate(final T resource);

}
