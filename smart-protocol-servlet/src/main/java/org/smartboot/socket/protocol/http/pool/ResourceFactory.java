package org.smartboot.socket.protocol.http.pool;

/**
 * An interface defining life-cycle methods for instances resources served by an
 * Pool.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * @param <T>
 *            Pooled Object.
 */
public interface ResourceFactory<T> {
	/**
	 * Build a new resource instance.
	 * 
	 * @return a new instance.
	 */
	public T create();

	/**
	 * Destroy the specified instance.
	 * 
	 * @param resource
	 *            instance to destroy
	 */
	public void destroy(final T resource);
}
