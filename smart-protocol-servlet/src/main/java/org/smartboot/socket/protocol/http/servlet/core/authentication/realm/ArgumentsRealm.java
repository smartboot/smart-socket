/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core.authentication.realm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationPrincipal;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationRealm;
import org.smartboot.socket.protocol.http.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Base class for authentication realms. Subclasses provide the source of
 * authentication roles, usernames, passwords, etc, and when asked for
 * validation respond with a role if valid, or null otherwise.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ArgumentsRealm.java,v 1.4 2007/06/01 15:55:41 rickknowles Exp $
 */
public class ArgumentsRealm implements AuthenticationRealm {

    private static final transient String PASSWORD_PREFIX = "argumentsRealm.passwd.";
    private static final transient String ROLES_PREFIX = "argumentsRealm.roles.";
    protected static Logger logger = LogManager.getLogger(ArgumentsRealm.class);
    private final Map<String, String> passwords;
    private final Map<String, List<String>> roles;

    /**
     * Constructor - this sets up an authentication realm, using the arguments
     * supplied on the command line as a source of userNames/passwords/roles.
     */
    public ArgumentsRealm(final Set<String> rolesAllowed, final Map<String, String> args) {
        passwords = new HashMap<String, String>();
        roles = new HashMap<String, List<String>>();
        for (final Iterator<String> i = args.keySet().iterator(); i.hasNext(); ) {
            final String key = i.next();
            if (key.startsWith(ArgumentsRealm.PASSWORD_PREFIX)) {
                final String userName = key.substring(ArgumentsRealm.PASSWORD_PREFIX.length());
                final String password = args.get(key);

                final String roleList = StringUtils.stringArg(args, ArgumentsRealm.ROLES_PREFIX + userName, "");
                if (roleList.equals("")) {
                    ArgumentsRealm.logger.warn("WARNING: No roles detected in configuration for user {}", userName);
                } else {
                    final StringTokenizer st = new StringTokenizer(roleList, ",");
                    final List<String> rl = new ArrayList<String>();
                    for (; st.hasMoreTokens(); ) {
                        final String currentRole = st.nextToken();
                        if (rolesAllowed.contains(currentRole)) {
                            rl.add(currentRole);
                        }
                    }
                    final String[] roleArray = (String[]) rl.toArray();
                    Arrays.sort(roleArray);
                    roles.put(userName, Arrays.asList(roleArray));
                }
                passwords.put(userName, password);
            }
        }
        ArgumentsRealm.logger.debug("ArgumentsRealm initialised: users: " + passwords.size());
    }

    /**
     * Authenticate the user - do we know them ? Return a principal once we know
     * them
     */
    @Override
    public AuthenticationPrincipal authenticateByUsernamePassword(final String userName, final String password) {
        if ((userName == null) || (password == null)) {
            return null;
        }

        final String realPassword = passwords.get(userName);
        if (realPassword == null) {
            return null;
        } else if (!realPassword.equals(password)) {
            return null;
        } else {
            return new AuthenticationPrincipal(userName, password, roles.get(userName));
        }
    }

    /**
     * Retrieve an authenticated user
     */
    @Override
    public AuthenticationPrincipal retrieveUser(final String userName) {
        if (userName == null) {
            return null;
        } else {
            return new AuthenticationPrincipal(userName, passwords.get(userName), roles.get(userName));
        }
    }
}
