/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core.authentication;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: AuthenticationHandler.java,v 1.2 2006/02/28 07:32:47
 *          rickknowles Exp $
 */
public interface AuthenticationHandler {
	/**
	 * Evaluates any authentication constraints, intercepting if auth is
	 * required. The relevant authentication handler subclass's logic is used to
	 * actually authenticate.
	 * 
	 * @return A boolean indicating whether to continue after this request
	 */
	public boolean processAuthentication(ServletRequest request, ServletResponse response, String pathRequested) throws IOException, ServletException;
}
