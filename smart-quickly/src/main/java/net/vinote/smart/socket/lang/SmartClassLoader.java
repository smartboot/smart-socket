package net.vinote.smart.socket.lang;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;

import net.vinote.smart.socket.logger.RunLogger;

public class SmartClassLoader extends ClassLoader {
	/**
	 * 配置classPath
	 */
	private String[] classPath;

	/**
	 * 是否支持子目录递归
	 */
	private boolean dirRecur;

	public SmartClassLoader(String classPath, ClassLoader parent) {
		super(parent);
		this.classPath = classPath.split(File.pathSeparator);
	}

	public SmartClassLoader(String classPath) {
		super();
		this.classPath = classPath.split(File.pathSeparator);
	}

	protected final Class<?> findClass(String name)
			throws ClassNotFoundException {
		InputStream in = null;
		try {
			String classFileName = name.replace('.', File.separatorChar)
					+ ".class";
			URL url = findResource(classFileName);
			if (url != null) {
				// Debug.println("load class", url.toString());
				in = url.openStream();
				ByteArrayOutputStream out = new ByteArrayOutputStream(
						in.available());
				byte[] b = new byte[1024];
				int length = -1;
				// 循环读取,单次读取类大小的数据流可能不完整
				while ((length = in.read(b)) != -1) {
					out.write(b, 0, length);
				}
				return defineClass(name, out.toByteArray(), 0, out.size());
			}
		} catch (IOException e) {
			RunLogger.getLogger().log(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					RunLogger.getLogger().log(e);
				}
			}
		}
		return super.findClass(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.ClassLoader#findResources(java.lang.String)
	 */
	protected Enumeration<URL> findResources(String name) throws IOException {
		List<URL> list = new ArrayList<URL>();
		URL url = findResource(name);
		if (url == null) {
			return super.findResources(name);
		}
		list.add(url);
		// if ("file".equals(url.getProtocol()))
		// {
		// File f = new File(url.getPath());
		// if (f.isDirectory())
		// {
		// for (File file : f.listFiles())
		// {
		// list.add(new URL("file:/" + file.getCanonicalPath()));
		// }
		// }
		// else
		// {
		// list.add(url);
		// }
		// }
		return list.size() > 0 ? new SmartEnumeration<URL>(list.iterator())
				: super.findResources(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.ClassLoader#findResource(java.lang.String)
	 */

	protected final URL findResource(String name) {
		String classJarUrl = name.replace(File.separatorChar, '/');
		for (String path : classPath) {

			File file = new File(path);
			if (!file.isDirectory()) {
				continue;
			}

			// 尝试加载class文件
			File classFile = new File(file, name);
			if (classFile.isFile() || classFile.isDirectory()) {
				try {
					return new URL("file:" + classFile.getAbsolutePath());
				} catch (MalformedURLException e) {
					RunLogger.getLogger().log(e);
				}
			}

			// 扫描jar包下的类
			URL url = locateURLFromJar(file, classJarUrl);
			if (url != null) {
				return url;
			}
		}
		return null;
	}

	/**
	 * 定位指定目录下的类URL
	 * 
	 * @param curPathFile
	 *            当前扫描目录
	 * @param name
	 *            扫描类文件
	 * @return
	 */
	private URL locateURLFromJar(File curPathFile, String classJarUrl) {

		if (!curPathFile.isDirectory()) {
			return null;
		}

		List<File> dirList = null;
		if (dirRecur) {
			dirList = new LinkedList<File>();
		}
		for (File file : curPathFile.listFiles()) {
			// 扫描当前目录下的jar包
			if (!file.isFile() || !file.getName().endsWith(".jar")) {
				if (dirRecur && file.isDirectory()) {
					dirList.add(file);// 存储子目录
				}
				continue;
			}
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(file);
				if (jarFile.getJarEntry(classJarUrl) != null) {
					return new URL("jar:file:" + file.getCanonicalPath() + "!/"
							+ classJarUrl);
				}
			} catch (IOException e) {
				RunLogger.getLogger().log(e);
			} finally {
				if (jarFile != null) {
					try {
						jarFile.close();
					} catch (IOException e) {
						RunLogger.getLogger().log(e);
					}
				}
			}

		}

		// 递归扫描子目录
		if (dirList != null) {
			for (File subPath : dirList) {
				URL url = locateURLFromJar(subPath, classJarUrl);
				if (url != null) {
					return url;
				}
			}
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		String path = System.getProperty("extPath");
		if (!new File(path).isDirectory()) {
			System.out.println("目录 " + path + " 不存在");
			return;
		}
		String clazz = System.getProperty("class");
		SmartClassLoader cl = new SmartClassLoader(path);
		Class<?> c = Class.forName(clazz, true, cl);
		Method m = c.getMethod("main", String[].class);
		m.invoke(null, (Object) args);
	}
}
