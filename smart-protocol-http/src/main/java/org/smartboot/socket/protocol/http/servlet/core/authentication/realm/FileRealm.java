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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author rickk
 * @version $Id: FileRealm.java,v 1.4 2006/08/30 04:07:52 rickknowles Exp $
 */
public class FileRealm implements AuthenticationRealm {

    private final static transient String FILE_NAME_ARGUMENT = "fileRealm.configFile";
    private final static transient String DEFAULT_FILE_NAME = "users.xml";
    private final static transient String ELEM_USER = "user";
    private final static transient String ATT_USERNAME = "username";
    private final static transient String ATT_PASSWORD = "password";
    private final static transient String ATT_ROLELIST = "roles";
    protected static Logger logger = LogManager.getLogger(FileRealm.class);
    private final Map<String, String> passwords;
    private final Map<String, List<String>> roles;

    /**
     * Constructor - this sets up an authentication realm, using the file
     * supplied on the command line as a source of userNames/passwords/roles.
     */
    public FileRealm(final Set<String> rolesAllowed, final Map<String, String> args) {
        passwords = new HashMap<String, String>();
        roles = new HashMap<String, List<String>>();

        // Get the filename and parse the xml doc
        final String realmFileName = args.get(FileRealm.FILE_NAME_ARGUMENT) == null ? FileRealm.DEFAULT_FILE_NAME : (String) args.get(FileRealm.FILE_NAME_ARGUMENT);
        final File realmFile = new File(realmFileName);
        if (!realmFile.exists()) {
            throw new RuntimeException("FileRealm could not locate the user file " + realmFile.getPath() + " - disabling security");
        }
        try {
            final InputStream inFile = new FileInputStream(realmFile);
            final Document doc = parseStreamToXML(inFile);
            inFile.close();
            final Node rootElm = doc.getDocumentElement();
            for (int n = 0; n < rootElm.getChildNodes().getLength(); n++) {
                final Node child = rootElm.getChildNodes().item(n);

                if ((child.getNodeType() == Node.ELEMENT_NODE) && (child.getNodeName().equals(FileRealm.ELEM_USER))) {
                    String userName = null;
                    String password = null;
                    String roleList = null;
                    // Loop through for attributes
                    for (int j = 0; j < child.getAttributes().getLength(); j++) {
                        final Node thisAtt = child.getAttributes().item(j);
                        if (thisAtt.getNodeName().equals(FileRealm.ATT_USERNAME)) {
                            userName = thisAtt.getNodeValue();
                        } else if (thisAtt.getNodeName().equals(FileRealm.ATT_PASSWORD)) {
                            password = thisAtt.getNodeValue();
                        } else if (thisAtt.getNodeName().equals(FileRealm.ATT_ROLELIST)) {
                            roleList = thisAtt.getNodeValue();
                        }
                    }

                    if ((userName == null) || (password == null) || (roleList == null)) {
                        FileRealm.logger.debug("Skipping user {} - details were incomplete", userName);
                    } else {
                        // Parse the role list into an array and sort it
                        final StringTokenizer st = new StringTokenizer(roleList, ",");
                        final List<String> rl = new ArrayList<String>();
                        for (; st.hasMoreTokens(); ) {
                            final String currentRole = st.nextToken();
                            if (rolesAllowed.contains(currentRole)) {
                                rl.add(currentRole);
                            }
                        }
                        final String[] roleArray = rl.toArray(new String[rl.size()]);
                        Arrays.sort(roleArray);
                        passwords.put(userName, password);
                        roles.put(userName, Arrays.asList(roleArray));
                    }
                }
            }
            FileRealm.logger.debug("FileRealm initialised: users:" + passwords.size());
        } catch (final java.io.IOException err) {
            throw new RuntimeException("Error loading FileRealm", err);
        }
    }

    /**
     * Get a parsed XML DOM from the given inputstream. Used to process the
     * web.xml application deployment descriptors.
     */
    private Document parseStreamToXML(final InputStream in) {
        try {
            // Use JAXP to create a document builder
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(Boolean.FALSE);
            factory.setValidating(Boolean.FALSE);
            factory.setNamespaceAware(Boolean.FALSE);
            factory.setIgnoringComments(Boolean.TRUE);
            factory.setCoalescing(Boolean.TRUE);
            factory.setIgnoringElementContentWhitespace(Boolean.TRUE);
            final DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (final Throwable errParser) {
            throw new RuntimeException("Error parsing the users XML document", errParser);
        }
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
        return new AuthenticationPrincipal(userName, passwords.get(userName), roles.get(userName));
    }
}
