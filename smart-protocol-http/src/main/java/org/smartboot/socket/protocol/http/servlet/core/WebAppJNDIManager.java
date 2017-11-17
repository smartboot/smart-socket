/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.jndi.JndiManager;
import org.w3c.dom.Node;

import javax.naming.NamingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Implements a simple web.xml + command line arguments style jndi manager
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WebAppJNDIManager.java,v 1.9 2006/02/28 07:32:48 rickknowles
 * Exp $
 */
public class WebAppJNDIManager {

    private final static transient String ELEM_ENV_ENTRY = "env-entry";
    private final static transient String ELEM_ENV_ENTRY_NAME = "env-entry-name";
    private final static transient String ELEM_ENV_ENTRY_TYPE = "env-entry-type";
    private final static transient String ELEM_ENV_ENTRY_VALUE = "env-entry-value";
    protected static Logger logger = LogManager.getLogger(WebAppJNDIManager.class);
    private final Set<String> objectCreated;
    private final JndiManager jndiManager;

    /**
     * Gets the relevant list of objects from the args, validating against the
     * web.xml nodes supplied. All node addresses are assumed to be relative to
     * the java:/comp/env context
     */
    public WebAppJNDIManager(final JndiManager jndiManager, final List<Node> webXMLNodes, final ClassLoader loader) {
        objectCreated = new HashSet<String>();
        this.jndiManager = jndiManager;
        // If the webXML nodes are not null, validate that all the entries we
        // wanted have been created
        if (webXMLNodes != null) {
            for (final Iterator<Node> i = webXMLNodes.iterator(); i.hasNext(); ) {
                final Node node = i.next();

                // Extract the env-entry nodes and create the objects
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                } else if (node.getNodeName().equals(WebAppJNDIManager.ELEM_ENV_ENTRY)) {
                    String name = null;
                    String className = null;
                    String value = null;
                    for (int m = 0; m < node.getChildNodes().getLength(); m++) {
                        final Node envNode = node.getChildNodes().item(m);
                        if (envNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        } else if (envNode.getNodeName().equals(WebAppJNDIManager.ELEM_ENV_ENTRY_NAME)) {
                            name = WebAppConfiguration.getTextFromNode(envNode);
                        } else if (envNode.getNodeName().equals(WebAppJNDIManager.ELEM_ENV_ENTRY_TYPE)) {
                            className = WebAppConfiguration.getTextFromNode(envNode);
                        } else if (envNode.getNodeName().equals(WebAppJNDIManager.ELEM_ENV_ENTRY_VALUE)) {
                            value = WebAppConfiguration.getTextFromNode(envNode);
                        }
                    }
                    if ((name != null) && (className != null) && (value != null)) {
                        WebAppJNDIManager.logger.debug("Creating object {} from web.xml env-entry description", name);
                        try {
                            jndiManager.bind(name, className, value, loader);
                            objectCreated.add(name);
                        } catch (final Throwable err) {
                            WebAppJNDIManager.logger.error("Error building JNDI object " + name + " (class: " + className + ")", err);
                        }
                    }
                }
            }
        }

    }

    public void destroy() {
        for (final String name : objectCreated) {
            try {
                jndiManager.unbind(name);
            } catch (final IllegalStateException ex) {
            } catch (final NamingException ex) {
            }
        }
        objectCreated.clear();
    }
}
