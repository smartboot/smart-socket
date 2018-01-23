/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.accesslog;


import org.smartboot.socket.protocol.http.servlet.core.WinstoneRequest;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneResponse;

/**
 * Used for logging accesses, eg in Apache access_log style
 * 
 * @author Jerome Guibert
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: AccessLogger.java,v 1.2 2006/02/28 07:32:47 rickknowles Exp $
 */
public interface AccessLogger {

	/**
	 * log access.
	 * 
	 * @param originalURL
	 * @param request
	 * @param response
	 */
	public void log(final String originalURL, final WinstoneRequest request, final WinstoneResponse response);
}
