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
import org.smartboot.socket.protocol.http.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Manages the references to individual hosts within the container. This object
 * handles the mapping of ip addresses and hostnames to groups of webapps, and
 * init and shutdown of any hosts it manages.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostGroup.java,v 1.4 2006/03/24 17:24:21 rickknowles Exp $
 */
public class HostGroup {

    private final static transient String DEFAULT_HOSTNAME = "default";
    protected static Logger logger = LogManager.getLogger(HostGroup.class);
    /**
     * map of host configuration
     */
    private final Map<String, HostConfiguration> hostConfigs;
    /**
     * jndi manager
     */
    private final JndiManager jndiManager;
    /**
     * arguments instance.
     */
    private final Map<String, String> args;
    /**
     * default host name if host mode is on
     */
    private String defaultHostName;

    /**
     * Build a new instance of HostGroup.
     *
     * @param jndiManager
     * @param args
     * @throws IOException
     */
    public HostGroup(final JndiManager jndiManager, final Map<String, String> args) throws IOException {
        hostConfigs = new HashMap<String, HostConfiguration>();
        this.jndiManager = jndiManager;
        this.args = args;
        // Is this the single or multiple configuration ? Check args
        final String hostDirName = StringUtils.stringArg(args, "hostsDir", null);
        final String webappsDirName = StringUtils.stringArg(args, "webappsDir", null);

        // If host mode
        if (hostDirName == null) {
            addHostConfiguration(webappsDirName, HostGroup.DEFAULT_HOSTNAME);
            defaultHostName = HostGroup.DEFAULT_HOSTNAME;
            HostGroup.logger.debug("Initialized in non-virtual-host mode");
        } else {
            // Otherwise multi-host mode
            initMultiHostDir(hostDirName);
            HostGroup.logger.debug("Initialized in virtual host mode with {} hosts: hostnames - {}", hostConfigs.size() + "", hostConfigs.keySet() + "");
        }
    }

    /**
     * @param hostname
     * @return HostConfiguration for specified hostname. if not found lookup for
     * defaultHostName.
     */
    public HostConfiguration getHostByName(final String hostname) {
        final HostConfiguration host = hostConfigs.get(hostname);
        return host != null ? host : hostConfigs.get(defaultHostName);
    }

    /**
     * Add an host configuration
     *
     * @param webappsDirName web application directory
     * @param hostname       host name
     */
    public void addHostConfiguration(final String webappsDirName, final String hostname) {
        HostGroup.logger.debug("Deploying host found at {}", hostname);
        final HostConfiguration config = new HostConfiguration(hostname, jndiManager, args, webappsDirName);
        hostConfigs.put(hostname, config);
    }

    /**
     * remove specified HostConfiguration.
     *
     * @param hostname
     */
    public void removeHostConfiguration(final String hostname) {
        if (hostConfigs.containsKey(hostname)) {
            hostConfigs.get(hostname).destroy();
            hostConfigs.remove(hostname);
        }
    }

    /**
     * Destroy HostGroup instance.
     */
    public void destroy() {
        if (hostConfigs != null) {
            // obtain a copy of name
            final Set<String> hostnames = new HashSet<String>(hostConfigs.keySet());
            for (final Iterator<String> i = hostnames.iterator(); i.hasNext(); ) {
                final String hostname = i.next();
                hostConfigs.get(hostname).destroy();
                hostConfigs.remove(hostname);
            }
            hostConfigs.clear();
        }
    }

    /**
     * Initialize a group host.
     *
     * @param hostsDirName
     * @throws WinstoneException if specified hosts directory is not found or not a directory.
     */
    protected final void initMultiHostDir(String hostsDirName) throws IOException, WinstoneException {
        if (hostsDirName == null) {
            // never reach in this implementation
            hostsDirName = "hosts";
        }
        final File hostsDir = new File(hostsDirName);
        if (!hostsDir.exists()) {
            throw new WinstoneException("Hosts dir " + hostsDirName + " not foundd");
        } else if (!hostsDir.isDirectory()) {
            throw new WinstoneException("Hosts dir " + hostsDirName + " is not a directory");
        } else {
            final File children[] = hostsDir.listFiles();
            if (children == null || children.length == 0) {
                throw new WinstoneException("Hosts dir " + hostsDirName + " is empty (no child webapps found)");
            }
            for (int n = 0; n < children.length; n++) {
                final String childName = children[n].getName();
                // Mount directories as host dirs
                if (children[n].isDirectory()) {
                    if (!hostConfigs.containsKey(childName)) {
                        addHostConfiguration(children[n].getCanonicalPath(), childName);
                    }
                }
                // set default host name
                if (defaultHostName == null || childName.equals(HostGroup.DEFAULT_HOSTNAME)) {
                    defaultHostName = childName;
                }
            }
        }
    }

    /**
     * Finalize threads.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            destroy();
        } catch (final Throwable e) {
        }
        super.finalize();
    }
}
