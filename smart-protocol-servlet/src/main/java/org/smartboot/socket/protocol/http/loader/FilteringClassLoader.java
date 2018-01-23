package org.smartboot.socket.protocol.http.loader;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * FilteringClassLoader.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class FilteringClassLoader extends ClassLoader {
	/** The packages to filter out, e.g. org.jboss.test.something */
	private String[] packages;

	/** The paths to filter out, e.g. org/jboss/test/something */
	private String[] paths;

	/**
	 * The urls to filter out, e.g.
	 * file:/home/whatever/project/output/classes/org/jboss/test/something
	 */
	private String[] urls;

	/**
	 * Create a new FilteringClassLoader.
	 * 
	 * @param parent
	 *            the parent classloader
	 * @param packages
	 *            the packages to filter out
	 */
	public FilteringClassLoader(ClassLoader parent, String[] packages) {
		super(parent);
		if (packages == null)
			throw new IllegalArgumentException("Null packages");
		this.packages = packages;

		// Determine the filtered paths
		paths = new String[packages.length];
		for (int i = 0; i < packages.length; ++i)
			paths[i] = packages[i].replace('.', '/');

		// Determine the filtered roots
		try {
			Enumeration<URL> enumeration = super.getResources("");
			List<URL> rootURLs = new ArrayList<URL>();
			while (enumeration.hasMoreElements())
				rootURLs.add(enumeration.nextElement());
			urls = new String[paths.length * rootURLs.size()];
			int i = 0;
			for (String path : paths) {
				for (URL rootURL : rootURLs)
					urls[i++] = new URL(rootURL, path).toString();
			}
		} catch (Exception e) {
			throw new RuntimeException("Error determining classloader urls", e);
		}
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		for (String pkg : packages) {
			if (name.startsWith(pkg))
				throw new ClassNotFoundException("Class not found (filtered): " + name + "\n set option useServerClassPath=false to disable filtering\n\n");
		}
		return super.loadClass(name, resolve);
	}

	@Override
	public URL getResource(String name) {
		for (String path : paths) {
			if (name.startsWith(path))
				return null;
		}
		return super.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL> unfiltered = super.getResources(name);
		List<URL> filtered = new ArrayList<URL>();
		while (unfiltered.hasMoreElements()) {
			URL next = unfiltered.nextElement();
			boolean ignore = false;
			for (String url : urls) {
				if (next.toString().startsWith(url))
					ignore = true;
			}
			if (ignore == false)
				filtered.add(next);
		}
		final Iterator<URL> iterator = filtered.iterator();
		return new Enumeration<URL>() {

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public URL nextElement() {
				return iterator.next();
			}

		};
	}
}