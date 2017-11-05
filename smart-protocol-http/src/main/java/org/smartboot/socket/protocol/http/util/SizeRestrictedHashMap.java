/**
 * 
 */
package org.smartboot.socket.protocol.http.util;

import java.util.Hashtable;
import java.util.Map;

/**
 * @see SizeRestrictedHashtable
 * @author Kohsuke Kawaguchi
 */
public class SizeRestrictedHashMap<K, V> extends Hashtable<K, V> {
	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 530449428559953521L;
	/**
	 * maximum capacity of this Hashtable.
	 */
	private final int maximumCapacity;

	/**
	 * 
	 * Build a new instance of SizeRestrictedHashMap.
	 * 
	 * @param initialCapacity
	 *            initial Capacity
	 * @param loadFactor
	 *            load Factor
	 * @param maximumCapacity
	 *            maximum Capacity
	 */
	public SizeRestrictedHashMap(final int initialCapacity, final float loadFactor, final int maximumCapacity) {
		super(initialCapacity, loadFactor);
		this.maximumCapacity = maximumCapacity;
	}

	public SizeRestrictedHashMap(final int initialCapacity, final int maximumCapacity) {
		super(initialCapacity);
		this.maximumCapacity = maximumCapacity;
	}

	public SizeRestrictedHashMap(final int maximumCapacity) {
		this.maximumCapacity = maximumCapacity;
	}

	public SizeRestrictedHashMap(final Map<? extends K, ? extends V> t, final int maximumCapacity) {
		super(t);
		this.maximumCapacity = maximumCapacity;
	}

	@Override
	public V put(final K key, final V value) {
		if (size() > maximumCapacity) {
			throw new IllegalStateException("Hash table size limit exceeded");
		}
		return super.put(key, value);
	}
}