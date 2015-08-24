package net.vinote.smart.socket.demo.http.loader;

import net.vinote.smart.socket.lang.SmartClassLoader;



public class SmartWebClassLoader extends SmartClassLoader {

	/**
	 * 数组中的包优先加载父级类加载器中的类
	 */
	private static final String[] PROTECTED_PACKAGE = { "java.",
			"javax.xml.transform.", "javax.xml.parsers.",
			"com.zjw.platform.quickly.", "org.xml.sax", "org.w3c.dom" };

	public SmartWebClassLoader(String classPath, ClassLoader parent) {
		super(classPath, parent);
	}

	public SmartWebClassLoader(String classPath) {
		super(classPath);
	}

	
	protected synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		boolean isProtected = isProtectedPackage(name);
		// 有限尝试自定义类加载器
		if (!isProtected) {
			Class<?> c = findLoadedClass(name);
			if (c == null || c.getClassLoader() != this) {
				try {
					c = findClass(name);
					if (resolve) {
						resolveClass(c);
					}
					return c;
				} catch (ClassNotFoundException e) {
					c = null;
				}
			}
		}
		try {
			return super.loadClass(name, resolve);
		} catch (ClassNotFoundException e) {
			// 父级类加载器未找到类，则尝试自定义类加载器
			if (isProtected) {
				Class<?> c = findLoadedClass(name);
				if (c == null || c.getClassLoader() != this) {
					c = findClass(name);
					if (resolve) {
						resolveClass(c);
					}
				}
				return c;
			} else {
				throw e;
			}
		}
	}

	private boolean isProtectedPackage(String name) {
		for (String pkg : PROTECTED_PACKAGE) {
			if (name.startsWith(pkg)) {
				return true;
			}
		}
		return false;
	}
}
