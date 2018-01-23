/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Implements the servlet spec model (v2.3 section 9.7.2) for classloading,
 * which is different to the standard JDK model in that it delegates *after*
 * checking local repositories. This has the effect of isolating copies of
 * classes that exist in 2 webapps from each other. Thanks to James Berry for
 * the changes to use the system classloader to prevent loading servlet spec or
 * system classpath classes again.<br />
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WebappClassLoader.java,v 1.4 2008/02/04 00:03:43 rickknowles
 * Exp $
 */
public class WebappClassLoader extends URLClassLoader {

    private final Logger logger = LogManager.getLogger(getClass());
    protected ClassLoader system = ClassLoader.getSystemClassLoader();

    public WebappClassLoader(final URL[] urls) {
        super(urls);
    }

    public WebappClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }

    public WebappClassLoader(final URL[] urls, final ClassLoader parent, final URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);

        // Try the system loader first, to ensure that system classes are not
        // overridden by webapps. Note that this includes any classes in
        // winstone,
        // including the javax.servlet classes
        if (c == null) {
            try {
                c = system.loadClass(name);
                if (c != null) {
                    logger.debug("Webapp classloader deferred to system classloader for loading {}", name);
                }
            } catch (final ClassNotFoundException e) {
                c = null;
            }
        }

        // If an allowed class, load it locally first
        if (c == null) {
            try {
                // If still not found, then invoke findClass in order to find
                // the class.
                c = findClass(name);
                if (c != null) {
                    logger.debug("Webapp classloader found class locally when loading {}", name);
                }
            } catch (final ClassNotFoundException e) {
                c = null;
            }
        }

        // otherwise, and only if we have a parent, delegate to our parent
        // Note that within winstone, the only difference between this and the
        // system
        // class loader we've already tried is that our parent might include the
        // common/shared lib.
        if (c == null) {
            final ClassLoader parent = getParent();
            if (parent != null) {
                c = parent.loadClass(name);
                if (c != null) {
                    logger.debug("Webapp classloader deferred to parent for loading {}", name);
                }
            } else {
                // We have no other hope for loading the class, so throw the
                // class not found exception
                throw new ClassNotFoundException(name);
            }
        }

        if (resolve && (c != null)) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        String pName = name;
        if ((name != null) && name.startsWith("/")) {
            pName = name.substring(1);
        }
        return super.getResourceAsStream(pName);
    }
}
