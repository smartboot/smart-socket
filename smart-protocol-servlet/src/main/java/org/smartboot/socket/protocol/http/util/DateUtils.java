package org.smartboot.socket.protocol.http.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
	private static final DateFormat gmtFormat;
	private static final SimpleDateFormat shortFormat;
	private static final SimpleDateFormat longFormat;

	static {
		shortFormat = new SimpleDateFormat("MMM dd HH:mm");
		longFormat = new SimpleDateFormat("MMM dd yyyy");
		gmtFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.US);
		DateUtils.gmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/**
	 * @param date
	 * @return a date string formatted in gmt style.
	 */
	public static String getGmt(final Date date) {
		return DateUtils.gmtFormat.format(date) + " GMT";
	}

	/**
	 * Returns a date string formatted in Unix ls style - if it's within six
	 * months of now, Mmm dd hh:ss, else Mmm dd yyyy.
	 * 
	 * @param date
	 * @return a date string formatted in Unix ls style.
	 */
	public static String lsDateStr(final Date date) {
		if (Math.abs(System.currentTimeMillis() - date.getTime()) < (183L * 24L * 60L * 60L * 1000L)) {
			return DateUtils.shortFormat.format(date);
		}
		return DateUtils.longFormat.format(date);
	}

}
