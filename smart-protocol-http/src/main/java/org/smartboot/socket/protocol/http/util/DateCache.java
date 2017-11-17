package org.smartboot.socket.protocol.http.util;

import java.text.DateFormat;
import java.util.Date;

/**
 * Computes String representations of Dates and caches the results so that
 * subsequent requests within the same second will be fast. Idea from Jetty
 * Util.
 * 
 * @author Jerome Guibert
 */
public final class DateCache {
	private final DateFormat dateFormat;
	private String result = null;
	private long lastTimeInSeconds = -1;

	public DateCache(final DateFormat format) {
		super();
		dateFormat = format;
	}

	public String now() {
		return format(System.currentTimeMillis());
	}

	public String format(final long time) {
		final long seconds = time / 1000;
		if ((lastTimeInSeconds < 0) || (lastTimeInSeconds != seconds)) {
			lastTimeInSeconds = time / 1000;
			final Date d = new Date(time);
			synchronized (dateFormat) {
				result = dateFormat.format(d);
			}
		}
		return result;
	}

}
