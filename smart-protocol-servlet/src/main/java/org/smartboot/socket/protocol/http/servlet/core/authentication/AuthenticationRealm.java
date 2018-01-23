/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core.authentication;

/**
 * Interface for authentication realms.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: AuthenticationRealm.java,v 1.3 2006/12/09 03:56:41 rickknowles
 *          Exp $
 */
public interface AuthenticationRealm {
	/**
	 * Authenticate the user - do we know them ? Return a distinct id once we
	 * know them. Used by the BASIC and FORM authentication methods.
	 */
	public AuthenticationPrincipal authenticateByUsernamePassword(String userName, String password);

	/**
	 * Retrieve an authenticated user. Used by the DIGEST and CLIENTCERT
	 * authentication methods.
	 */
	public AuthenticationPrincipal retrieveUser(String userName);
}
