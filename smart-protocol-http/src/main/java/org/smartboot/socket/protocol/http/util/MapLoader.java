package org.smartboot.socket.protocol.http.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Load a resourceBundle and build a map with all keys/values.
 * 
 * @author Jerome Guibert
 */
public final class MapLoader {

	/**
	 * Load the specified resource bundle and build a map with all keys finded.
	 * 
	 * @param resourceBundle
	 *            the specified resource bundle
	 * @return a <code>Map</code> instance representing key/value found in the
	 *         specified resource bundle.
	 */
	public static Map<String, String> load(final ResourceBundle resourceBundle) {
		final Map<String, String> resources = new HashMap<String, String>();
		if (resourceBundle != null) {
			final Enumeration<String> keys = resourceBundle.getKeys();
			String key = null;
			while (keys.hasMoreElements()) {
				key = keys.nextElement();
				final String value = resourceBundle.getString(key);
				resources.put(key, value);
			}
		}
		return resources;
	}
}
