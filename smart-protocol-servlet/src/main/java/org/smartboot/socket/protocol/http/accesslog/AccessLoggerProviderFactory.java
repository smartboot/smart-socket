package org.smartboot.socket.protocol.http.accesslog;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * AccessLoggerProviderFactory instantiate AccessLoggerProvider using
 * ServiceLoader provided by JDK 1.6.
 * 
 * @author Jerome guibert
 */
public final class AccessLoggerProviderFactory {

	private final AccessLoggerProvider provider;

	/**
	 * Singleton Holder Pattern.
	 */
	private static class AccessLoggerProviderFactoryHolder {

		private static AccessLoggerProviderFactory loggerFactory = new AccessLoggerProviderFactory();
	}

	public static AccessLogger getAccessLogger(final String host, final String webapp, final PatternType patternType, final String filePattern) {
		return AccessLoggerProviderFactoryHolder.loggerFactory.provider.getAccessLogger(host, webapp, patternType, filePattern);
	}

	public static void destroy(final AccessLogger accessLogger) {
		AccessLoggerProviderFactoryHolder.loggerFactory.provider.destroy(accessLogger);
	}

	/**
	 * Build a new instance of AccessLoggerProviderFactory.
	 */
	private AccessLoggerProviderFactory() {
		super();
		final ServiceLoader<AccessLoggerProvider> loader = ServiceLoader.load(AccessLoggerProvider.class);
		final Iterator<AccessLoggerProvider> iterator = loader.iterator();
		AccessLoggerProvider accessLoggerProvider = null;
		while ((accessLoggerProvider == null) && iterator.hasNext()) {
			try {
				accessLoggerProvider = iterator.next();
			} catch (final Throwable e) {
			}
		}
		if (accessLoggerProvider == null) {
			throw new Error("No Access Logger Provider registered");
		}
		provider = accessLoggerProvider;
	}
}
