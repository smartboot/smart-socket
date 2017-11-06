package org.smartboot.socket.protocol.http.jndi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.jndi.resources.DataSourceConfig;
import org.smartboot.socket.protocol.http.jndi.resources.SimpleDatasource;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Jndi Manager
 *
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class JndiManager {

    protected Logger logger = LogManager.getLogger(getClass());
    protected InitialContext initialContext;
    protected ScheduledExecutorService scheduler;

    public JndiManager() {
        super();
    }

    /**
     * Initialize context factory.
     *
     * @throws NamingException
     */

    public void initialize() {
        // Instantiate scheduler with a initial pool size of one thread.
        scheduler = Executors.newScheduledThreadPool(1);
        // initiate context factory
        final Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "net.winstone.jndi.url.java.javaURLContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "net.winstone.jndi.url");
        try {
            initialContext = new InitialContext(env);
        } catch (final NamingException e) {
            throw new IllegalStateException(e);
        }
        logger.info("jndi context initialized");
    }

    /**
     * Clean destroy.
     *
     * @throws NamingException
     */

    public void destroy() {
        if (initialContext != null) {
            // close datasource
            try {
                final Context jdbc = (Context) initialContext.lookup("java:/comp/env/jdbc");
                final NamingEnumeration<NameClassPair> names = jdbc.list("");
                while (names.hasMore()) {
                    try {
                        final NameClassPair pair = names.next();
                        final Object object = jdbc.lookup(pair.getName());
                        // is a winstone datasource ?
                        if (object instanceof SimpleDatasource) {
                            // close it
                            ((SimpleDatasource) object).close();
                        }
                        // unbind datasource
                        jdbc.unbind(pair.getName());
                    } catch (final NamingException e) {
                    }
                }
            } catch (final NamingException e) {
                throw new IllegalStateException(e);
            }
            // close initial context
            try {
                initialContext.close();
                initialContext = null;
            } catch (final NamingException e) {
            }
        }
        // stop scheduler
        if (!scheduler.isShutdown()) {
            // stop scheduled
            scheduler.shutdownNow();
        }
        logger.info("jndi context destroyed");
    }

    /**
     * Create and bind a datasource in Naming context.
     *
     * @param dataSourceConfig the datasource configuration to bindSmtpSession.
     * @throws IllegalStateException If Jndi manager is closed
     * @throws NamingException       If binding already exists.
     */
    public void bind(final DataSourceConfig dataSourceConfig) throws IllegalStateException, NamingException {
        final SimpleDatasource dataSource = new SimpleDatasource(dataSourceConfig);
        String jndiName = dataSource.getName();
        if (jndiName.startsWith("jdbc/")) {
            jndiName = "java:/comp/env/" + jndiName;
        }
        bind(jndiName, dataSource);
        if (dataSourceConfig.getKeepAlivePeriod() > 0) {
            scheduler.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    dataSource.keepAlive();
                }
            }, dataSourceConfig.getKeepAlivePeriod(), dataSourceConfig.getKeepAlivePeriod(), TimeUnit.MINUTES);
        }
        if (dataSourceConfig.getKillInactivePeriod() > 0) {
            scheduler.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    dataSource.drain();
                }
            }, dataSourceConfig.getKillInactivePeriod(), dataSourceConfig.getKillInactivePeriod(), TimeUnit.MINUTES);
        }
    }

    /**
     * Create and bind a mail session under java:comp/env/mail.
     *
     * @param name
     * @param properties
     * @param loader
     * @throws IllegalStateException
     * @throws NamingException
     */
    public void bindSmtpSession(final String name, final Properties properties, final ClassLoader loader) throws IllegalStateException, NamingException {
        try {
            final Class<?> smtpClass = Class.forName("javax.mail.Session", Boolean.TRUE, loader);
            final Method smtpMethod = smtpClass.getMethod("getInstance", new Class[]{Properties.class, Class.forName("javax.mail.Authenticator")});
            // create object
            final Object object = smtpMethod.invoke(null, new Object[]{properties, null});
            String jndiName = name;
            if (name.startsWith("mail/")) {
                jndiName = "java:comp/env/" + name;
            }
            // bind it
            bind(jndiName, object);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (final SecurityException e) {
            throw new IllegalStateException(e);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (final IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create and bind an simple object under "java:/comp/env/"
     *
     * @param name      name of binding
     * @param className class name
     * @param value     object value
     * @param loader    class loader to use
     * @throws IllegalStateException
     * @throws NamingException
     */
    public void bind(final String name, final String className, final String value, final ClassLoader loader) throws IllegalStateException, NamingException {
        if (value != null) {
            try {
                // load class
                final Class<?> objClass = Class.forName(className.trim(), Boolean.TRUE, loader);
                // find constructor
                final Constructor<?> objConstr = objClass.getConstructor(new Class[]{String.class});
                // create object
                final Object object = objConstr.newInstance(new Object[]{value});
                // bind it
                bind(name, object);
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (final IllegalArgumentException e) {
                throw new IllegalStateException(e);
            } catch (final InstantiationException e) {
                throw new IllegalStateException(e);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e);
            } catch (final SecurityException e) {
                throw new IllegalStateException(e);
            } catch (final NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Unbind specifed name under "java:/comp/env/".
     *
     * @param name
     * @throws IllegalStateException
     * @throws NamingException
     */
    public void unbind(final String name) throws IllegalStateException, NamingException {
        if (name != null) {
            String jndiName = name;
            if (!jndiName.startsWith("java:/comp/env/")) {
                jndiName = "java:/comp/env/" + jndiName;
            }
            try {
                initialContext.unbind(jndiName);
            } catch (final IllegalArgumentException e) {
                throw new IllegalStateException(e);
            } catch (final SecurityException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public InitialContext getInitialContext() {
        return initialContext;
    }

    /**
     * Utility method to bind an object under "java:/comp/env/": we build all
     * needed sub context.
     *
     * @param name   the name
     * @param object object to bindSmtpSession
     * @throws IllegalStateException If Jndi manager is closed
     * @throws NamingException
     */
    private void bind(final String name, final Object object) throws IllegalStateException, NamingException {
        if (initialContext == null) {
            throw new IllegalStateException("Initial Context is closed");
        }
        String jndiName = name;
        if (!jndiName.startsWith("java:/comp/env/")) {
            jndiName = "java:/comp/env/" + jndiName;
        }
        Name fullName = new CompositeName(jndiName);
        Context currentContext = initialContext;
        while (fullName.size() > 1) {
            // Make contexts that are not already present
            try {
                currentContext = currentContext.createSubcontext(fullName.get(0));
            } catch (final NamingException err) {
                currentContext = (Context) currentContext.lookup(fullName.get(0));
            }
            fullName = fullName.getSuffix(1);
        }
        initialContext.bind(name, object);
        logger.info("bind " + jndiName + " to " + object);
    }
}
