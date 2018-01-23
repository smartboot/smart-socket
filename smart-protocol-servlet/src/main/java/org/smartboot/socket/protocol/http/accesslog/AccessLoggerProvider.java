package org.smartboot.socket.protocol.http.accesslog;

/**
 * AccessLoggerProvider add basic method in order to manage life cycle of
 * AccessLogger.
 * 
 * @author Jerome Guibert
 */
public interface AccessLoggerProvider {

	public AccessLogger getAccessLogger(final String host, final String webapp, final PatternType patternType, final String filePattern);

	public void destroy(final AccessLogger accessLogger);
}
