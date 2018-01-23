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
import org.smartboot.socket.protocol.http.util.FileUtils;
import org.smartboot.socket.protocol.http.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Manages the references to individual webapps within the container. This
 * object handles the mapping of url-prefixes to webapps, and init and shutdown
 * of any webapps it manages.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostConfiguration.java,v 1.8 2007/08/02 06:16:00 rickknowles
 * Exp $
 */
public class HostConfiguration {

    /**
     * TIME PERIOD (ms) OF SESSION FLUSHING
     */
    private static final long FLUSH_PERIOD = 60000L;
    private static final String WEB_INF = "WEB-INF";
    private static final String WEB_XML = "web.xml";
    protected static Logger logger = LogManager.getLogger(HostConfiguration.class);
    /**
     * host name/
     */
    private final String hostname;
    /**
     * Arguments map.
     */
    private final Map<String, String> args;
    /**
     * Map of WebAppConfiguration, key is context path of web application
     */
    private final Map<String, WebAppConfiguration> webapps;

    /**
     * JNDI Manager instance.
     */
    private final JndiManager jndiManager;

    /**
     * Thread instance in order to flush sessions.
     */
    private Thread thread;

    /**
     * Build a new instance of HostConfiguration.
     *
     * @param hostname
     * @param jndiManager
     * @param args
     * @param webappsDirName
     */
    public HostConfiguration(final String hostname, final JndiManager jndiManager, final Map<String, String> args, final String webappsDirName) {
        webapps = new HashMap<String, WebAppConfiguration>();
        /** load configuration */
        this.hostname = hostname;
        this.args = args;
        this.jndiManager = jndiManager;
        /**
         * For now we can keep this mode single/multiple
         */
        if (webappsDirName == null) {
            // Single web application
            final File warfile = StringUtils.fileArg(args, "warfile");
            final File webroot = StringUtils.fileArg(args, "webroot");
            if (warfile != null || webroot != null) {
                final String prefix = StringUtils.stringArg(args, "prefix", "");
                try {
                    addWebAppConfiguration(prefix, getWebRoot(webroot, warfile), "webapp");
                } catch (final IOException e) {
                    HostConfiguration.logger.error("Error initializing web application: prefix [" + prefix + "]", e);
                }
            }
            // Several webroot on different locations
            final String webroots = StringUtils.stringArg(args, "webroots", null);
            if (webroots != null) {
                final StringTokenizer tokenizer = new StringTokenizer(webroots, ";");
                while (tokenizer.hasMoreTokens()) {
                    final String root = tokenizer.nextToken();
                    if (!"".equals(root)) {
                        final File froot = new File(root);
                        if (!froot.exists()) {
                            HostConfiguration.logger.warn("WebRoot {} not exist. Skipping it", root);
                        } else {
                            deploy(froot, Boolean.FALSE);
                        }
                    }
                }
            }
        } else {
            // one directory with multiple webapp
            final File webappsDir = new File(webappsDirName);
            if (!webappsDir.exists()) {
                throw new WinstoneException("Webapps dir " + webappsDirName + " not found");
            } else if (!webappsDir.isDirectory()) {
                throw new WinstoneException("Webapps dir " + webappsDirName + " is not a directory");
            } else {
                for (final File aChildren : webappsDir.listFiles()) {
                    deploy(aChildren, Boolean.TRUE);
                }
            }
        }
        HostConfiguration.logger.debug("Initialized {} webapps: prefixes - {}", webapps.size() + "", webapps.keySet() + "");

        // initialize demaon to invalidate session
        thread = new Thread(new Runnable() {

            /**
             * Main method of thread which invalidate Expired Sessions every
             * 60s.
             *
             * @see Runnable#run()
             */
            @Override
            public void run() {
                boolean interrupted = Boolean.FALSE;
                while (!interrupted) {
                    try {
                        Thread.sleep(HostConfiguration.FLUSH_PERIOD);
                        invalidateExpiredSessions();
                    } catch (final InterruptedException err) {
                        interrupted = Boolean.TRUE;
                    }
                }
                thread = null;
            }
        }, "WinstoneHostConfigurationMgmt:" + this.hostname);
        thread.setDaemon(Boolean.TRUE);
        thread.start();
    }

    /**
     * @param prefix      path * @param webRoot web root file
     * @param contextName context name
     * @return a WebAppConfiguration instance.
     * @throws WinstoneException if context name is ever used
     */
    public WebAppConfiguration addWebAppConfiguration(final String prefix, final File webRoot, final String contextName) throws WinstoneException {
        if (webapps.containsKey(prefix)) {
            throw new WinstoneException("Prefix " + prefix + " is ever used");
        }
        WebAppConfiguration webAppConfiguration = null;
        try {
            webAppConfiguration = initWebApp(prefix, webRoot, contextName);
            webapps.put(webAppConfiguration.getContextPath(), webAppConfiguration);
            HostConfiguration.logger.info("Deploy web application: prefix [{}] webroot [{}]", prefix, webRoot);
        } catch (final IOException e) {
            HostConfiguration.logger.error("Error initializing web application: prefix [" + prefix + "]", e);
        }
        return webAppConfiguration;
    }

    /**
     * @param uri
     * @return a WebAppConfiguration for specified uri or null if none is found.
     */
    public WebAppConfiguration getWebAppByURI(final String uri) {
        if (uri == null) {
            return null;
        } else if (uri.equals("/") || uri.equals("")) {
            return webapps.get("");
        } else if (uri.startsWith("/")) {
            final String decoded = WinstoneRequest.decodeURLToken(uri);
            final String noLeadingSlash = decoded.substring(1);
            final int slashPos = noLeadingSlash.indexOf("/");
            if (slashPos == -1) {
                return webapps.get(decoded);
            } else {
                return webapps.get(decoded.substring(0, slashPos + 1));
            }
        } else {
            return null;
        }
    }

    /**
     * @return a set of context names.
     */
    public Set<String> getContextNames() {
        return webapps.keySet();
    }

    /**
     * Reload Specified Webapplication.
     *
     * @param prefix
     * @throws IOException
     */
    public void reloadWebApp(final String prefix) throws IOException {
        final WebAppConfiguration webAppConfig = webapps.get(prefix);
        if (webAppConfig != null) {
            final String webRoot = webAppConfig.getWebroot();
            final String contextName = webAppConfig.getContextName();
            destroyWebApp(prefix);
            try {
                final WebAppConfiguration webAppConfiguration = initWebApp(prefix, new File(webRoot), contextName);
                webapps.put(webAppConfiguration.getContextPath(), webAppConfiguration);
            } catch (final Throwable err) {
                HostConfiguration.logger.error("Error initializing web application: prefix [" + prefix + "]", err);
            }
        } else {
            throw new WinstoneException("Unknown webapp prefix: " + prefix);
        }
    }

    /**
     * Initialize specified webapplication.
     *
     * @param prefix      path
     * @param webRoot     web root file
     * @param contextName context name
     * @return a WebAppConfiguration instance.
     * @throws IOException
     */
    protected final WebAppConfiguration initWebApp(final String prefix, final File webRoot, final String contextName) throws IOException {
        Node webXMLParentNode = null;
        final File webInfFolder = new File(webRoot, HostConfiguration.WEB_INF);
        if (webInfFolder.exists()) {
            final File webXmlFile = new File(webInfFolder, HostConfiguration.WEB_XML);
            if (webXmlFile.exists()) {
                HostConfiguration.logger.debug("Parsing web.xml");
                final Document webXMLDoc = new WebXmlParser().parseStreamToXML(webXmlFile);
                if (webXMLDoc != null) {
                    webXMLParentNode = webXMLDoc.getDocumentElement();
                    HostConfiguration.logger.debug("Finished parsing web.xml");
                } else {
                    HostConfiguration.logger.debug("Failure parsing the web.xml file. Ignoring and continuing as if no web.xml was found.");

                }
            }
        }
        // Instantiate the webAppConfig
        return new WebAppConfiguration(this, jndiManager, webRoot.getCanonicalPath(), prefix, args, webXMLParentNode, contextName);
    }

    /**
     * @return host name
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Destroy this webapp instance. Kills the webapps, plus any servlets,
     * attributes, etc
     */
    private void destroyWebApp(final String prefix) {
        final WebAppConfiguration webAppConfig = webapps.get(prefix);
        if (webAppConfig != null) {
            webAppConfig.destroy();
            webapps.remove(prefix);
        }
    }

    /**
     * Destroy all webapplication.
     */
    public void destroy() {
        final Set<String> prefixes = new HashSet<String>(webapps.keySet());
        for (final String prefixe : prefixes) {
            destroyWebApp(prefixe);
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Invalidate all expired sessions.
     */
    private void invalidateExpiredSessions() {
        for (final WebAppConfiguration webapp : webapps.values()) {
            webapp.invalidateExpiredSessions();
        }
    }

    /**
     * Setup the webroot. If a warfile is supplied, extract any files that the
     * war file is newer than. If none is supplied, use the default temp
     * directory.
     *
     * @param requestedWebroot
     * @param warfile
     * @return
     * @throws IOException
     */
    private File getWebRoot(final File requestedWebroot, final File warfile) throws IOException {
        if (warfile != null) {
            HostConfiguration.logger.info("Beginning extraction from war file");
            // open the war file
            if (!warfile.exists() || !warfile.isFile()) {
                throw new WinstoneException("The warfile supplied is unavailable or invalid (" + warfile + ")");
            }

            // Get the webroot folder (or a temp dir if none supplied)
            File unzippedDir = null;
            if (requestedWebroot != null) {
                unzippedDir = requestedWebroot;
            } else {
                // compute which temp directory to use
                String tempDirectory = StringUtils.stringArg(args, "tempDirectory", null);
                String child = "winstone" + File.separator;
                if (tempDirectory == null) {
                    // find default temp directory
                    // System.getProperty("");
                    final File tempFile = File.createTempFile("dummy", "dummy");
                    tempDirectory = tempFile.getParent();
                    tempFile.delete();
                    final String userName = System.getProperty("user.name");
                    if (userName != null) {
                        child += StringUtils.replace(userName, new String[][]{{"/", ""}, {"\\", ""}, {",", ""}}) + File.separator;
                    }
                }
                if (hostname != null) {
                    child += hostname + File.separator;
                }
                child += warfile.getName();
                unzippedDir = new File(tempDirectory, child);
            }
            if (unzippedDir.exists()) {
                if (!unzippedDir.isDirectory()) {
                    throw new WinstoneException("The webroot supplied is not a valid directory (" + unzippedDir.getPath() + ")");
                } else {
                    HostConfiguration.logger.debug("The webroot supplied already exists - overwriting where newer ({})", unzippedDir.getCanonicalPath());
                }
            }

            // check consistency and if out-of-sync, recreate
            final File timestampFile = new File(unzippedDir, ".timestamp");
            if (!timestampFile.exists() || Math.abs(timestampFile.lastModified() - warfile.lastModified()) > 1000) {
                // contents of the target directory is inconsistent from the
                // war.
                FileUtils.delete(unzippedDir);
                unzippedDir.mkdirs();
            } else {
                // files are up to date
                return unzippedDir;
            }

            // Iterate through the files
            final JarFile warArchive = new JarFile(warfile);
            for (final Enumeration<JarEntry> e = warArchive.entries(); e.hasMoreElements(); ) {
                final JarEntry element = e.nextElement();
                if (element.isDirectory()) {
                    continue;
                }
                final String elemName = element.getName();

                // If archive date is newer than unzipped file, overwrite
                final File outFile = new File(unzippedDir, elemName);
                if (outFile.exists() && outFile.lastModified() > warfile.lastModified()) {
                    continue;
                }

                outFile.getParentFile().mkdirs();
                final byte buffer[] = new byte[8192];

                // Copy out the extracted file
                final InputStream inContent = warArchive.getInputStream(element);
                final OutputStream outStream = new FileOutputStream(outFile);
                int readBytes = inContent.read(buffer);
                while (readBytes != -1) {
                    outStream.write(buffer, 0, readBytes);
                    readBytes = inContent.read(buffer);
                }
                inContent.close();
                outStream.close();
            }
            // extraction completed
            new FileOutputStream(timestampFile).close();
            timestampFile.setLastModified(warfile.lastModified());

            // Return webroot
            return unzippedDir;
        } else {
            return requestedWebroot;
        }
    }

    /**
     * Deploy a Webapplication war or folder.
     *
     * @param aChildren
     * @param checkMatchingWarfile
     */
    private void deploy(final File aChildren, final boolean checkMatchingWarfile) {
        final String childName = aChildren.getName();
        // Check any directories for warfiles that match, and skip: only
        // deploy the war file
        if (aChildren.isDirectory()) {
            final File matchingWarFile = new File(aChildren.getParentFile(), aChildren.getName() + ".war");
            if (checkMatchingWarfile && matchingWarFile.exists() && matchingWarFile.isFile()) {
                HostConfiguration.logger.debug("Webapp dir deployment {} skipped, since there is a war file of the same name to check for re-extraction", childName);
            } else {
                final String prefix = childName.equalsIgnoreCase("ROOT") ? "" : "/" + childName;
                if (!webapps.containsKey(prefix)) {
                    final WebAppConfiguration webAppConfig = addWebAppConfiguration(prefix, aChildren, childName);
                    if (webAppConfig != null) {
                        HostConfiguration.logger.info("Deployed web application found at {}", aChildren.getAbsolutePath());
                    }
                }
            }
        } else if (childName.endsWith(".war")) {
            final String outputName = childName.substring(0, childName.lastIndexOf(".war"));
            final String prefix = outputName.equalsIgnoreCase("ROOT") ? "" : "/" + outputName;

            if (!webapps.containsKey(prefix)) {
                final File outputDir = new File(aChildren.getParentFile(), outputName);
                outputDir.mkdirs();
                WebAppConfiguration webAppConfig = null;
                try {
                    webAppConfig = addWebAppConfiguration(prefix, getWebRoot(new File(aChildren.getParentFile(), outputName), aChildren), outputName);
                } catch (final IOException e) {
                    HostConfiguration.logger.error("Error initializing web application: prefix [" + prefix + "]", e);
                }
                if (webAppConfig != null) {
                    HostConfiguration.logger.info("Deployed web application found at {}", aChildren.getAbsolutePath());
                }
            }
        }
    }

    /**
     * @param sessionKey
     * @return a WebAppConfiguration instance for specified session key or null
     * if none was found.
     */
    public WebAppConfiguration getWebAppBySessionKey(final String sessionKey) {
        for (final WebAppConfiguration webAppConfiguration : webapps.values()) {
            final WinstoneSession session = webAppConfiguration.getSessionById(sessionKey, Boolean.FALSE);
            if (session != null) {
                return webAppConfiguration;
            }
        }
        return null;
    }
}
