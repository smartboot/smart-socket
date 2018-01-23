/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core.authentication;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;

/**
 * Implements the principal method - basically just a way of identifying an
 * authenticated user.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: AuthenticationPrincipal.java,v 1.2 2006/02/28 07:32:47
 *          rickknowles Exp $
 */
public class AuthenticationPrincipal implements Principal, Serializable {
	private static final long serialVersionUID = 3716429852671570273L;
	private final String userName;
	private final String password;
	private final List<String> roles;
	private String authenticationType;

	/**
	 * Constructor
	 */
	public AuthenticationPrincipal(final String userName, final String password, final List<String> roles) {
		this.userName = userName;
		this.password = password;
		this.roles = roles;
	}

	@Override
	public String getName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getAuthType() {
		return authenticationType;
	}

	public void setAuthType(final String authType) {
		authenticationType = authType;
	}

	/**
	 * Searches for the requested role in this user's roleset.
	 */
	public boolean isUserIsInRole(final String role) {
		if (roles == null) {
			return Boolean.FALSE;
		} else if (role == null) {
			return Boolean.FALSE;
		} else {
			return roles.contains(role);
		}
	}
}
