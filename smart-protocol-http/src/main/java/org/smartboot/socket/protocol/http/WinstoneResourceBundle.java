package org.smartboot.socket.protocol.http;


import org.smartboot.socket.protocol.http.util.MapLoader;
import org.smartboot.socket.protocol.http.util.StringUtils;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * A ResourceBundle that includes the ability to do string replacement on the
 * resources it retrieves (based on Rick Knowles), and where all properties are
 * loaded in memory.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class WinstoneResourceBundle implements Iterable<String> {

	protected final Map<String, String> resources;

	/**
	 * 
	 * WinstoneResourceBundleHolder. Use initialization on demand class holder.
	 * 
	 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
	 * 
	 */
	private static class WinstoneResourceBundleHolder {
		private static WinstoneResourceBundle bundle = new WinstoneResourceBundle("winstone-message");
	}

	/**
	 * @return a WinstoneResourceBundle instance.
	 */
	public static WinstoneResourceBundle getInstance() {
		return WinstoneResourceBundleHolder.bundle;
	}

	/**
	 * 
	 * Build a new instance of WinstoneResourceBundle.
	 * 
	 * @param baseName
	 */
	public WinstoneResourceBundle(final String baseName) {
		this(ResourceBundle.getBundle(baseName));
	}

	/**
	 * 
	 * Build a new instance of WinstoneResourceBundle.
	 * 
	 * @param baseName
	 * @param locale
	 */
	public WinstoneResourceBundle(final String baseName, final Locale locale) {
		this(ResourceBundle.getBundle(baseName, locale));
	}

	/**
	 * 
	 * Build a new instance of WinstoneResourceBundle.
	 * 
	 * @param baseName
	 * @param locale
	 * @param classLoader
	 */
	public WinstoneResourceBundle(final String baseName, final Locale locale, final ClassLoader classLoader) {
		this(ResourceBundle.getBundle(baseName, locale, classLoader));
	}

	/**
	 * 
	 * Build a new instance of WinstoneResourceBundle.
	 * 
	 * @param resourceBundle
	 */
	public WinstoneResourceBundle(final ResourceBundle resourceBundle) {
		super();
		resources = MapLoader.load(resourceBundle);
	}

	/**
	 * @see Iterable#iterator()
	 */
	@Override
	public Iterator<String> iterator() {
		return resources.keySet().iterator();
	}

	/**
	 * WinstoneResourceBundle implement Iterable<String>.
	 *
	 * @see Iterable#iterator()
	 */
	@Deprecated
	public Iterable<String> getKeys() {
		return resources.keySet();
	}

	/**
	 * Default getString method
	 */
	public String getString(final String key) {
		return resources.get(key);
	}

	/**
	 * Perform a string replace for a set of from/to pairs.
	 */
	public String getString(final String key, final String... parameters) {
		return StringUtils.replaceToken(resources.get(key), parameters);
	}
}
