/**
 * 
 */
package org.smartboot.socket.protocol.http.util;

import java.util.Hashtable;
import java.util.Map;

/**
 * {@link Hashtable} that sets the upper bound in the total number of keys.
 * 
 * This is to protect against the DoS attack based on the hash key collision.
 * See http://www.ocert.org/advisories/ocert-2011-003.html
 * 
 * @author Kohsuke Kawaguchi
 */
public class SizeRestrictedHashtable<K, V> extends Hashtable<K, V> {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -3783614228146621688L;
	/**
	 * maximum Capacity.
	 */
	private final int maximumCapacity;

	public SizeRestrictedHashtable(final int initialCapacity, final float loadFactor, final int cap) {
		super(initialCapacity, loadFactor);
		this.maximumCapacity = cap;
	}

	public SizeRestrictedHashtable(final int initialCapacity, final int cap) {
		super(initialCapacity);
		this.maximumCapacity = cap;
	}

	public SizeRestrictedHashtable(final int cap) {
		this.maximumCapacity = cap;
	}

	public SizeRestrictedHashtable(final Map<? extends K, ? extends V> t, final int cap) {
		super(t);
		this.maximumCapacity = cap;
	}

	@Override
	public V put(final K key, final V value) {
		if (size() > maximumCapacity) {
			throw new IllegalStateException("Hash table size limit exceeded");
		}
		return super.put(key, value);
	}
}