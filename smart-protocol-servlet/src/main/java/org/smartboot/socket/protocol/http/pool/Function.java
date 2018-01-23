package org.smartboot.socket.protocol.http.pool;

/**
 * Interface to declare function.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * @param <R>
 *            Returned type
 * @param <P>
 *            Parameter type
 */
public interface Function<R, P> {
	/**
	 * Apply a process on 'from' parameter.
	 * 
	 * @param from
	 * @return result.
	 */
	public R apply(P from);
}
