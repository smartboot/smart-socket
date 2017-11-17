/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.servlet.core.WebAppConfiguration;
import org.smartboot.socket.protocol.http.util.StringUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This subclass of WinstoneClassLoader is the reloading version. It runs a
 * monitoring thread in the background that checks for updates to any files in
 * the class path.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ReloadingClassLoader.java,v 1.11 2007/02/17 01:55:12
 * rickknowles Exp $
 */
public class ReloadingClassLoader extends WebappClassLoader implements ServletContextListener, Runnable {

    private static final int RELOAD_SEARCH_SLEEP = 10;
    private final Set<String> loadedClasses;
    protected Logger logger = LogManager.getLogger(getClass());
    private boolean interrupted;
    private WebAppConfiguration webAppConfig;
    private File classPaths[];
    private int classPathsLength;

    public ReloadingClassLoader(final URL urls[], final ClassLoader parent) {
        super(urls, parent);
        loadedClasses = new HashSet<String>();
        if (urls != null) {
            classPaths = new File[urls.length];
            for (int n = 0; n < urls.length; n++) {
                classPaths[classPathsLength++] = new File(urls[n].getFile());
            }
        }
    }

    private static String transformToFileFormat(final String name) {
        if (!name.startsWith("Class:")) {
            return name;
        }
        return StringUtils.replace(name.substring(6), ".", "/") + ".class";
    }

    @Override
    protected void addURL(final URL url) {
        super.addURL(url);
        synchronized (loadedClasses) {
            if (classPaths == null) {
                classPaths = new File[10];
                classPathsLength = 0;
            } else if (classPathsLength == (classPaths.length - 1)) {
                final File temp[] = classPaths;
                classPaths = new File[(int) (classPathsLength * 1.75)];
                System.arraycopy(temp, 0, classPaths, 0, classPathsLength);
            }
            classPaths[classPathsLength++] = new File(url.getFile());
        }
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        webAppConfig = (WebAppConfiguration) sce.getServletContext();
        interrupted = Boolean.FALSE;
        synchronized (this) {
            loadedClasses.clear();
        }
        final Thread thread = new Thread(this, "WinstoneClassLoader Reloading Monitor Thread");
        thread.setDaemon(Boolean.TRUE);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        interrupted = Boolean.TRUE;
        webAppConfig = null;
        synchronized (this) {
            loadedClasses.clear();
        }
    }

    /**
     * The maintenance thread. This makes sure that any changes in the files in
     * the classpath trigger a classLoader self destruct and recreate.
     */
    @Override
    public void run() {
        logger.info("WinstoneClassLoader reloading monitor thread started");

        final Map<String, Long> classDateTable = new HashMap<String, Long>();
        final Map<String, File> classLocationTable = new HashMap<String, File>();
        final Set<String> lostClasses = new HashSet<String>();
        while (!interrupted) {
            try {
                String loadedClassesCopy[] = null;
                synchronized (this) {
                    loadedClassesCopy = loadedClasses.toArray(new String[0]);
                }

                for (int n = 0; (n < loadedClassesCopy.length) && !interrupted; n++) {
                    Thread.sleep(ReloadingClassLoader.RELOAD_SEARCH_SLEEP);
                    final String className = ReloadingClassLoader.transformToFileFormat(loadedClassesCopy[n]);
                    final File location = classLocationTable.get(className);
                    Long classDate = null;
                    if ((location == null) || !location.exists()) {
                        for (int j = 0; (j < classPaths.length) && (classDate == null); j++) {
                            final File path = classPaths[j];
                            if (!path.exists()) {
                                continue;
                            } else if (path.isDirectory()) {
                                final File classLocation = new File(path, className);
                                if (classLocation.exists()) {
                                    classDate = new Long(classLocation.lastModified());
                                    classLocationTable.put(className, classLocation);
                                }
                            } else if (path.isFile()) {
                                classDate = searchJarPath(className, path);
                                if (classDate != null) {
                                    classLocationTable.put(className, path);
                                }
                            }
                        }
                    } else if (location.exists()) {
                        classDate = new Long(location.lastModified());
                    }

                    // Has class vanished ? Leave a note and skip over it
                    if (classDate == null) {
                        if (!lostClasses.contains(className)) {
                            lostClasses.add(className);
                            logger.debug("WARNING: Maintenance thread can't find class {} - Lost ? Ignoring", className);
                        }
                        continue;
                    }
                    if ((classDate != null) && lostClasses.contains(className)) {
                        lostClasses.remove(className);
                    }

                    // Stash date of loaded files, and compare with last
                    // iteration
                    final Long oldClassDate = classDateTable.get(className);
                    if (oldClassDate == null) {
                        classDateTable.put(className, classDate);
                    } else if (oldClassDate.compareTo(classDate) != 0) {
                        // Trigger reset of webAppConfig
                        logger.info("Class {} changed at {} (old date {}) - reloading", new Object[]{className, new Date(classDate.longValue()).toString(), new Date(oldClassDate.longValue()).toString()});
                        webAppConfig.resetClassLoader();
                    }
                }
            } catch (final Throwable err) {
                logger.error("Error in WinstoneClassLoader reloading monitor thread", err);
            }
        }
        logger.info("WinstoneClassLoader reloading monitor thread finished");
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        synchronized (this) {
            loadedClasses.add("Class:" + name);
        }
        return super.findClass(name);
    }

    @Override
    public URL findResource(final String name) {
        synchronized (this) {
            loadedClasses.add(name);
        }
        return super.findResource(name);
    }

    /**
     * Iterates through a jar file searching for a class. If found, it returns
     * that classes date
     */
    private Long searchJarPath(final String classResourceName, final File path) throws IOException, InterruptedException {
        final JarFile jar = new JarFile(path);
        for (final Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements() && !interrupted; ) {
            final JarEntry entry = e.nextElement();
            if (entry.getName().equals(classResourceName)) {
                return new Long(path.lastModified());
            }
        }
        return null;
    }
}
