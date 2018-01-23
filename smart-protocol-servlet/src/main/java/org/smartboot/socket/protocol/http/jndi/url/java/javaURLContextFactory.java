package org.smartboot.socket.protocol.http.jndi.url.java;


import org.smartboot.socket.protocol.http.jndi.NamingContext;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * javaURLContextFactory class start with "java" and not "Java" according to
 * jndi naming scheme:
 * <p>
 * <code>
 * prefix.schemeId.schemeIdURLContextFactory 
 * </code>
 * </p>
 * See
 * <a>http://java.sun.com/products/jndi/tutorial/beyond/env/overview.html</a>
 * <ul>
 * <li>java:comp/env Application environment entries</li>
 * <li>java:comp/env/jdbc JDBC DataSource resource manager connection factories</li>
 * <li>java:comp/env/mail JavaMail Session Connection Factories</li>
 * </ul>
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class javaURLContextFactory implements InitialContextFactory, ObjectFactory {

	private static Context rootContext;

	public javaURLContextFactory() {
		super();
	}

	public javaURLContextFactory(final Hashtable<?, ?> environment) {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Context getInitialContext(final Hashtable<?, ?> environnement) throws NamingException {
		if (javaURLContextFactory.rootContext == null) {
			javaURLContextFactory.rootContext = new NamingContext((Hashtable<String, Object>) environnement);
			final Context env = javaURLContextFactory.rootContext.createSubcontext("java:").createSubcontext("comp").createSubcontext("env");
			// TODO we should delegate this to the Jndi Manager
			env.createSubcontext("jdbc");
			env.createSubcontext("mail");
			// TODO choose how map host and webapp in jndi context
			// env.createSubcontext("hosts");
		}
		return (Context) javaURLContextFactory.rootContext.lookup("java:/comp/env");
	}

	@Override
	public Object getObjectInstance(final Object urlInfo, final Name name, final Context context, final Hashtable<?, ?> environnement) throws Exception {
		// Case 1: urlInfo is null
		// This means to create a URL context that can accept arbitrary "foo"
		// URLs.
		if (urlInfo == null) {
			return getInitialContext(environnement);
		}
		// this tell Object Manager to try on other factory.
		return null;
	}
}
