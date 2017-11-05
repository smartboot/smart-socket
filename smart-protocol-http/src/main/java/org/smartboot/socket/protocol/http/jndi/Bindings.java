package org.smartboot.socket.protocol.http.jndi;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * 
 * Bindings.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class Bindings implements NamingEnumeration<Binding> {

	private Iterator<Entry<String, Object>> iterator;
	private Context context;
	private Hashtable<String, Object> environnement;

	public Bindings(final Context context, final Hashtable<String, Object> environnement, final Map<String, Object> bindings) {
		super();
		this.context = context;
		this.environnement = environnement;
		iterator = bindings.entrySet().iterator();
	}

	@Override
	public void close() throws NamingException {
		context = null;
		environnement = null;
		iterator = null;
	}

	@Override
	public boolean hasMore() throws NamingException {
		if (iterator == null) {
			throw new NamingException("Enumeration has already been closed");
		}
		return iterator.hasNext();
	}

	@Override
	public Binding next() throws NamingException {
		if (hasMore()) {
			final Entry<String, Object> entry = iterator.next();

			final String name = entry.getKey();
			Object value = entry.getValue();
			try {
				value = NamingManager.getObjectInstance(value, new CompositeName().add(name), context, environnement);
			} catch (final Throwable err) {
				final NamingException errNaming = new NamingException("Failed To Get Instance ");
				errNaming.setRootCause(err);
				throw errNaming;
			}
			return new Binding(name, value);
		}
		throw new NoSuchElementException();
	}

	@Override
	public boolean hasMoreElements() {
		try {
			return hasMore();
		} catch (final NamingException err) {
			return Boolean.FALSE;
		}
	}

	@Override
	public Binding nextElement() {
		try {
			return next();
		} catch (final NamingException namingException) {
			throw new NoSuchElementException();
		}
	}

}
