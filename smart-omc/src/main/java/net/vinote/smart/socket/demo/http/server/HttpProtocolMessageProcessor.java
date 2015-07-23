package net.vinote.smart.socket.demo.http.server;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.vinote.smart.socket.demo.http.application.AbstractServletContext;
import net.vinote.smart.socket.demo.http.application.DeveloperServerApplication;
import net.vinote.smart.socket.demo.http.loader.SmartWebClassLoader;
import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.http.RequestUnit;
import net.vinote.smart.socket.protocol.http.SmartHttpRequest;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 服务器消息处理器,由服务器启动时构造
 * 
 * @author Seer
 * 
 */
public class HttpProtocolMessageProcessor extends AbstractProtocolDataProcessor {
	private static final RunLogger logger = RunLogger.getLogger();

	// private ProtocolProcessThread[] processThreads;
	private static final String NO_CONTEXT_SERVLET = "NO_CONTEXT_SERVLET";

	private Map<String, ArrayBlockingQueue<RequestUnit>> msgMap = new HashMap<String, ArrayBlockingQueue<RequestUnit>>();

	private Map<String, Object> appMap = new HashMap<String, Object>();

	private DeveloperServerApplication invalidApp = new DeveloperServerApplication();

	private Map<String, HttpServlet> invalidServletMap = new HashMap<String, HttpServlet>();
	{
		HttpServlet servlet = new HttpServlet() {
			/**
			 * 
			 */
			private static final long serialVersionUID = -6512522558130873643L;

			/*
			 * (non-Javadoc)
			 * 
			 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.
			 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
			 */
			protected void doGet(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException,
					IOException {
				resp.setStatus(404);
				resp.getWriter().print("404");
			}
		};
		invalidServletMap.put("/*", servlet);
		try {
			invalidApp.registerServlets(invalidServletMap);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HttpProtocolMessageProcessor(AbstractServletContext app) {
		appMap.put(app.getServerName(), app);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.zjw.platform.quickly.process.MessageProcessor#process(com.zjw.platform
	 * .quickly.Session, com.zjw.platform.quickly.message.DataEntry)
	 */
	
	public boolean receive(TransportSession tsession, DataEntry msg) {
		RequestUnit unit = new RequestUnit((SmartHttpRequest) msg, tsession);
		ArrayBlockingQueue<RequestUnit> queue = msgMap.get(unit.getRequest()
				.getContextPath());
		if (queue != null) {
			return queue.offer(unit);
		} else {
			return msgMap.get(NO_CONTEXT_SERVLET).offer(unit);
		}
	}

	
	public void init(QuicklyConfig config) throws Exception {
		super.init(config);

		String webAppDir = System.getProperty("Smart_Home") + File.separator
				+ "webapps";
		File appDir = new File(webAppDir);
		if (!appDir.isDirectory()) {
			throw new RuntimeException();
		}
		File[] appFiles = appDir.listFiles(new FileFilter() {

			
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		for (final File f : appFiles) {
			// 构造当前应用的类加载器
			SmartWebClassLoader scl = new SmartWebClassLoader(
					System.getProperty("Smart_Home") + File.separator + "lib"
							+ File.pathSeparator + f.getAbsolutePath()
							+ File.separatorChar + "WEB-INF"
							+ File.separatorChar + "classes"
							+ File.pathSeparator + f.getAbsolutePath()
							+ File.separatorChar + "WEB-INF"
							+ File.separatorChar + "lib",
					ClassLoader.getSystemClassLoader());

			final Class<?> initializer = scl
					.loadClass("com.zjw.platform.module.http.application.SmartServerApplication");
			final Object obj = initializer.newInstance();

			// 消息存储队列
			ArrayBlockingQueue<RequestUnit> msgQueue = new ArrayBlockingQueue<RequestUnit>(
					1024);
			msgMap.put("/" + f.getName(), msgQueue);

			// 启动处理线程
			HttpMessageProcessThread[] processThreads = new HttpMessageProcessThread[config
					.getThreadNum()];
			boolean init = false;
			for (int i = 0; i < processThreads.length; i++) {

				processThreads[i] = new HttpMessageProcessThread(
						"Http-Process[" + f.getName() + "]-Thread-" + i, this,
						msgQueue);
				if (!init) {
					Handler handler = new Handler() {

						
						public void handler() {
							try {
								initializer.getMethod("init", String.class)
										.invoke(obj, f.getAbsolutePath());
								initializer.getMethod("setServerName",
										String.class).invoke(obj, f.getName());
							} catch (Exception e) {
								logger.log(Level.SEVERE, "", e);
							}
						}
					};
					processThreads[i].setHandler(handler);
					init = true;
				}
				processThreads[i].setPriority(Thread.MAX_PRIORITY);
				processThreads[i].setContextClassLoader(scl);
				processThreads[i].start();
			}
			appMap.put("/" + f.getName(), obj);
		}

	}

	
	public void shutdown() {
		/*
		 * for (ProtocolProcessThread thread : processThreads) {
		 * thread.shutdown(); }
		 */
		ServiceProcessorManager.getInstance().destory();
	}

	
	public <T> void notifyProcess(T session) {
		throw new UnsupportedOperationException();
	}

	
	public <T> void process(T msg) throws Exception {
		try {
			RequestUnit unit = (RequestUnit) msg;
			Object obj = appMap.get(unit.getRequest().getContextPath());
			if (obj != null) {
				Method method = obj.getClass().getMethod("servie",
						RequestUnit.class);
				method.invoke(obj, unit);
			} else {
				invalidApp.servie(unit);
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	
	public ClusterMessageEntry generateClusterMessage(DataEntry data) {
		return null;
	}

}
