package org.smartboot.socket.protocol.http.accesslog;

/**
 * SimpleAccessLoggerProvider implementation.
 * 
 * @author Jerome Guibert
 */
public final class SimpleAccessLoggerProvider implements AccessLoggerProvider {

	public SimpleAccessLoggerProvider() {
		super();
	}

	@Override
	public void destroy(final AccessLogger accessLogger) {
		if (accessLogger != null) {
			if (accessLogger instanceof SimpleAccessLogger) {
				((SimpleAccessLogger) accessLogger).destroy();
			}
		}
	}

	@Override
	public AccessLogger getAccessLogger(final String host, final String webapp, final PatternType patternType, final String filePattern) {
		return new SimpleAccessLogger(host, webapp, patternType, filePattern);
	}
}
