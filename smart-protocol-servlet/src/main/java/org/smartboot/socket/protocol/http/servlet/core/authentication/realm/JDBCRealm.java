/*
 * Copyright 2006 Rui Damas <rui.damas at gmail com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core.authentication.realm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationPrincipal;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationRealm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A JDBC authentication realm to be used with Winstone Servelet container.
 * <p>
 * --JDBCRealm.url and --JDBCRealm.user are required.
 * </p>
 *
 * @author Rui Damas
 */
public class JDBCRealm implements AuthenticationRealm {

    // Command line arguments to SQL identifiers
    public static final transient String ARGS_USER_REL = "JDBCRealm.userRel";
    // Defaults for SQL identifiers
    public static final transient String DEFAULT_USER_REL = "web_users";
    // Command line arguments prefix
    // Command line arguments for connecting
    private static final transient String ARGS_DRIVER = "JDBCRealm.driver";
    private static final transient String ARGS_URL = "JDBCRealm.url";
    private static final transient String ARGS_USER = "JDBCRealm.user";
    private static final transient String ARGS_PASSWORD = "JDBCRealm.password";
    private static final transient String ARGS_USER_NAME_COL = "JDBCRealm.userNameCol";
    private static final transient String ARGS_USER_CRED_COL = "JDBCRealm.userCredCol";
    private static final transient String ARGS_USER_ROLE_REL = "JDBCRealm.userRoleRel";
    private static final transient String ARGS_ROLE_NAME_COL = "JDBCRealm.roleNameCol";
    private static final transient String DEFAULT_USER_NAME_COL = "username";
    private static final transient String DEFAULT_USER_CRED_COL = "credential";
    private static final transient String DEFAULT_USER_ROLE_REL = "web_user_roles";
    private static final transient String DEFAULT_ROLE_NAME_COL = "rolename";
    protected static Logger logger = LogManager.getLogger(JDBCRealm.class);
    private final String url;
    private final String user;
    private final String password;
    private final String retriveUserQuery;
    private final String authenticationQueryPostfix;
    private final String userRolesQuery;
    private Connection connection;

    /**
     * Creates a new instance of JDBCAuthenticationRealm.
     * <p>
     * If a <code>"JDBCRealm.driver"</code> exists in the <code>args</code> map
     * an atempt to load the class will be made and a success message will be
     * printed to <code>System.out</code>, or, if the class fails to load, an
     * error message will be printed to <code>System.err</code>.
     * </p>
     */
    public JDBCRealm(final Set<String> rolesAllowed, final Map<String, String> args) {
        // Get connection arguments
        final String driver = args.get(JDBCRealm.ARGS_DRIVER);
        url = args.get(JDBCRealm.ARGS_URL);
        user = args.get(JDBCRealm.ARGS_USER);
        password = args.get(JDBCRealm.ARGS_PASSWORD);

        // Get SQL identifier arguments
        String userRel = args.get(JDBCRealm.ARGS_USER_REL), userNameCol = args.get(JDBCRealm.ARGS_USER_NAME_COL), userCredCol = args.get(JDBCRealm.ARGS_USER_CRED_COL), userRoleRel = args.get(JDBCRealm.ARGS_USER_ROLE_REL), roleNameCol = args
                .get(JDBCRealm.ARGS_ROLE_NAME_COL);

        // Get defaults if necessary
        if (userRel == null) {
            userRel = JDBCRealm.DEFAULT_USER_REL;
        }
        if (userNameCol == null) {
            userNameCol = JDBCRealm.DEFAULT_USER_NAME_COL;
        }
        if (userCredCol == null) {
            userCredCol = JDBCRealm.DEFAULT_USER_CRED_COL;
        }
        if (userRoleRel == null) {
            userRoleRel = JDBCRealm.DEFAULT_USER_ROLE_REL;
        }
        if (roleNameCol == null) {
            roleNameCol = JDBCRealm.DEFAULT_ROLE_NAME_COL;
        }

        retriveUserQuery = "SELECT 1\n" + "  FROM \"" + userRel + "\"\n" + "  WHERE \"" + userNameCol + "\" = ?";

        // Prepare query prefixes
        authenticationQueryPostfix = "\n    AND \"" + userCredCol + "\" = ?";

        userRolesQuery = "SELECT \"" + roleNameCol + "\"\n" + "  FROM \"" + userRoleRel + "\"\n" + "  WHERE \"" + userNameCol + "\" = ?";

        // If the driver was specified
        if (driver != null) {
            try {
                // Try to load the driver
                Class.forName(driver);
                // and notify if loaded
                JDBCRealm.logger.info("JDBCRealm loaded jdbc driver: " + driver);
            } catch (final ClassNotFoundException cnfe) {
                // Notify if fails
                JDBCRealm.logger.error("JDBCRealm failed to load jdbc driver: " + driver);
            }
        }
    }

    public AuthenticationPrincipal getPrincipal(final String userName, final String password, final boolean usePassword) {
        try {
            // Get a connection
            if ((connection == null) || connection.isClosed()) {
                connection = DriverManager.getConnection(url, user, this.password);
            }
            // Query for user
            String query = retriveUserQuery;
            if (usePassword) {
                query = query + authenticationQueryPostfix;
            }
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, userName);
            if (usePassword) {
                ps.setString(2, password);
            }
            ResultSet resultSet = ps.executeQuery();
            // If there is a user (row)
            if (resultSet.next()) {
                // Query for the user roles
                query = userRolesQuery;
                ps = connection.prepareStatement(query);
                ps.setString(1, userName);
                resultSet = ps.executeQuery();
                // Load list
                final List<String> roles = new ArrayList<String>();
                while (resultSet.next()) {
                    roles.add(resultSet.getString(1));
                }
                return new AuthenticationPrincipal(userName, password, roles);
            }
        } catch (final SQLException sqle) {
            JDBCRealm.logger.error("getPrincipal Error:", sqle);
        }
        return null;
    }

    /**
     * Authenticate the user - do we know them ? Return a distinct id once we
     * know them.
     *
     * @return <code>getPrincipal(userName, password, Boolean.TRUE);</code>
     */
    @Override
    public AuthenticationPrincipal authenticateByUsernamePassword(final String userName, final String password) {
        return getPrincipal(userName, password, Boolean.TRUE);
    }

    /**
     * Retrieve an authenticated user
     *
     * @return <code>getPrincipal(userName, password, Boolean.FALSE);</code>
     */
    @Override
    public AuthenticationPrincipal retrieveUser(final String userName) {
        return getPrincipal(userName, null, Boolean.FALSE);
    }
}
