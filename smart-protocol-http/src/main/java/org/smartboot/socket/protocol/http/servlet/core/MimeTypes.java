package org.smartboot.socket.protocol.http.servlet.core;


import org.smartboot.socket.protocol.http.util.MapLoader;

import java.net.FileNameMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * MimeTypes implement a FileNameMap and using a <code>MapLoader</code> in order
 * to load data (localized on file MimeTypes.properties). MimeTypes is
 * initialized on demand (@see
 * http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom).
 * 
 *  @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class MimeTypes implements FileNameMap {

	private static final transient String UNKNOWN_EXTENSION = "unknown/unknown";

	/** an unmodifiable map of Mime type. */
	private final Map<String, String> ressource;

	/**
	 * Build a new instance of MimeTypes.
	 */
	private MimeTypes() {
		this(Collections.unmodifiableMap(MapLoader.load(ResourceBundle.getBundle("mime"))));
	}

	/**
	 * 
	 * Build a new instance of MimeTypes.
	 * @param ressource
	 */
	private MimeTypes(final Map<String, String> ressource) {
		super();
		this.ressource = ressource;
	}

	/**
	 * Gets the MIME type for the specified extension.
	 * 
	 * @param extension
	 *            the specified extension
	 * @return <code>String</code> indicating the MIME type for the specified
	 *         extension.
	 */
	public String getContentType(final String extension) {
		String contentType = ressource.get(extension.toLowerCase(Locale.ENGLISH));
		if (contentType == null) {
			contentType = MimeTypes.UNKNOWN_EXTENSION;
		}
		return contentType;
	}

	@Override
	public String getContentTypeFor(final String fileName) {
		final int dotPos = fileName.lastIndexOf('.');
		if ((dotPos != -1) && (dotPos != (fileName.length() - 1))) {
			final String extension = fileName.substring(dotPos + 1).toLowerCase();
			return getContentType(extension);
		} else {
			// no extension => no content type.
			return null;
		}
	}

	/** Use initialization on demand class holder. */
	private static class MimeTypesLazyHolder {
		private static MimeTypes uniqueInstance = new MimeTypes();
	}

	/**
	 * @return a instance of <code>MimeTypes</code> using default mime type
	 *         information.
	 */
	public static MimeTypes getInstance() {
		return MimeTypesLazyHolder.uniqueInstance;
	}

	/**
	 * Build a MimeTypes with default and the specified additional MimeType.
	 * 
	 * @param additionalMimeType
	 *            the specified additional MimeType
	 * @return a instance of MimeTypes.
	 */
	public static MimeTypes getInstance(final Map<String, String> additionalMimeType) {
		final MimeTypes mimeTypes = MimeTypes.getInstance();
		if ((additionalMimeType == null) || additionalMimeType.isEmpty()) {
			return mimeTypes;
		}
		final Map<String, String> customized = new HashMap<String, String>(additionalMimeType.size() + mimeTypes.ressource.size());
		customized.putAll(mimeTypes.ressource);
		customized.putAll(additionalMimeType);
		return new MimeTypes(customized);
	}

}
