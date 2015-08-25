package net.vinote.smart.socket.demo.http.application;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;
import javax.xml.bind.JAXBException;

import net.vinote.smart.socket.demo.http.filter.SmartFilterConfig;
import net.vinote.smart.socket.demo.http.filter.SmartFilterModel;
import net.vinote.smart.socket.demo.http.filter.SmartServletConfig;
import net.vinote.smart.socket.demo.http.filter.SmartServletModel;
import net.vinote.smart.socket.logger.RunLogger;

import com.sun.java.xml.ns.javaee.FilterMappingType;
import com.sun.java.xml.ns.javaee.FilterType;
import com.sun.java.xml.ns.javaee.ListenerType;
import com.sun.java.xml.ns.javaee.ParamValueType;
import com.sun.java.xml.ns.javaee.ServletMappingType;
import com.sun.java.xml.ns.javaee.ServletNameType;
import com.sun.java.xml.ns.javaee.ServletType;
import com.sun.java.xml.ns.javaee.UrlPatternType;

public class SmartServerApplication extends AbstractServletContext {

	public SmartServerApplication() {
		/*
		 * if (this.getClass().getClassLoader().getClass() !=
		 * SmartWebClassLoader.class) { throw new
		 * RuntimeException("Invalid classloader"); }
		 */
	}

	public void init0(String serverPath) throws Exception {
		// this.serverPath = serverPath;
		// realPath = serverPath;
		WebXmlConfig webXmlConfig = null;
		try {
			webXmlConfig = new WebXmlConfig(serverPath + File.separatorChar
					+ "WEB-INF" + File.separatorChar + "web.xml");
		} catch (JAXBException e) {
			RunLogger.getLogger().log(e);
		}
		RunLogger.getLogger().log(
				Level.SEVERE,
				Thread.currentThread() + " ; "
						+ Thread.currentThread().getContextClassLoader() + " "
						+ this.getClass().getClassLoader());

		renderServetContext(webXmlConfig);
		renderListener(webXmlConfig);
		renderServlet(webXmlConfig);
		renderFilter(webXmlConfig);
	}

	private void renderServetContext(WebXmlConfig webXmlConfig) {
		// TODO Auto-generated method stub

	}

	private void renderListener(WebXmlConfig webXmlConfig) throws Exception {
		List<ListenerType> filterList = webXmlConfig.getListenerList();
		for (ListenerType type : filterList) {
			Class<?> listenerClass = Class.forName(type.getListenerClass()
					.getValue());
			Object listener = listenerClass.newInstance();
			if (listener instanceof ServletContextListener) {
				servletContextListenerList
						.add((ServletContextListener) listener);
			} else if (listener instanceof ServletContextAttributeListener) {
				servletContextAttributeListenerList
						.add((ServletContextAttributeListener) listener);
			} else if (listener instanceof HttpSessionListener) {
				httpSessionListenerList.add((HttpSessionListener) listener);
			} else if (listener instanceof HttpSessionAttributeListener) {
				httpSessionAttributeListenerList
						.add((HttpSessionAttributeListener) listener);
			} else if (listener instanceof HttpSessionActivationListener) {
				httpSessionActivationListenerList
						.add((HttpSessionActivationListener) listener);
			} else if (listener instanceof HttpSessionBindingListener) {
				httpSessionBindingListenerList
						.add((HttpSessionBindingListener) listener);
			} else if (listener instanceof ServletRequestListener) {
				servletRequestListenerList
						.add((ServletRequestListener) listener);
			} else if (listener instanceof ServletRequestAttributeListener) {
				servletRequestAttributeListenerList
						.add((ServletRequestAttributeListener) listener);
			} else {
				throw new RuntimeException();
			}
		}
	}

	public static void main(String[] args) {
		System.out.println(ServletContextListener.class
				.isInstance(ServletContextListener.class));
	}

	private void renderServlet(WebXmlConfig webXmlConfig) throws Exception {
		List<ServletType> servletList = webXmlConfig.getServletList();
		List<ServletMappingType> servletMappingList = webXmlConfig
				.getServletMappingList();
		Collections.sort(servletList, new Comparator<ServletType>() {

			public int compare(ServletType o1, ServletType o2) {
				int first = Integer.parseInt(o1.getLoadOnStartup());
				int second = Integer.parseInt(o2.getLoadOnStartup());
				return first - second;
			}
		});
		for (ServletType servletType : servletList) {
			// 当前filter对应的mapping
			Map<String, URLPatternType> patterMap = new HashMap<String, URLPatternType>();
			for (ServletMappingType type : servletMappingList) {
				if (!type.getServletName().getValue()
						.equals(servletType.getServletName().getValue())) {
					continue;
				}
				for (UrlPatternType urlType : type.getUrlPattern()) {
					String url = urlType.getValue();
					if (!validateURLPattern(url)) {
						throw new IllegalArgumentException(
								"invalid url-mapping:" + url);
					}
					// 精确路径匹配
					if (url.startsWith("/") && (url.indexOf("*") < 0)) {
						patterMap.put(url, URLPatternType.AllMatcher);
					}
					// 目录匹配
					else if (url.startsWith("/") && (url.indexOf("*") > 0)) {
						patterMap.put(url.replace("*", ".*"),
								URLPatternType.CatalogMatcher);
					}
					// 扩展名匹配
					else if (url.startsWith("*.")) {
						patterMap.put(url.replace(".", "(.)")
								.replace("*", ".*"),
								URLPatternType.ExtensionMatcher);
					} else {
						throw new IllegalArgumentException(
								"invalid url-mapping:" + url);
					}
				}
			}

			// 解析servlet配置
			List<ParamValueType> pvList = servletType.getInitParam();
			Map<String, String> parameterMap = new HashMap<String, String>(
					pvList.size());
			for (ParamValueType type : pvList) {
				parameterMap.put(type.getParamName().getValue(), type
						.getParamValue().getValue());
			}
			SmartServletConfig config = new SmartServletConfig(servletType
					.getServletName().getValue(), this, parameterMap);
			List<ParamValueType> paramList = servletType.getInitParam();
			for (ParamValueType type : paramList) {
				config.setInitParameter(type.getParamName().getValue(), type
						.getParamValue().getValue());
			}

			Class<?> servletClass = Class.forName(servletType.getServletClass()
					.getValue());
			Servlet servlet = (Servlet) servletClass.newInstance();

			super.servletList.put(
					config.getServletName(),
					new SmartServletModel(patterMap, servlet, config, Integer
							.parseInt(servletType.getLoadOnStartup()) >= 0));
		}
	}

	private void renderFilter(WebXmlConfig webXmlConfig) throws Exception {
		List<FilterType> filterList = webXmlConfig.getFilterList();
		List<FilterMappingType> filterMappingList = webXmlConfig
				.getFilterMappingList();
		for (FilterType type : filterList) {
			// 过滤器参数
			SmartFilterConfig config = new SmartFilterConfig(type
					.getFilterName().getValue(), this);
			List<ParamValueType> paramList = type.getInitParam();
			for (ParamValueType pvType : paramList) {
				config.setParameter(pvType.getParamName().getValue(), pvType
						.getParamValue().getValue());
			}

			Class<?> filterClass = Class.forName(type.getFilterClass()
					.getValue());
			Filter filter = (Filter) filterClass.newInstance();
			filter.init(config);
			// 当前filter对应的mapping
			Map<String, URLPatternType> patterMap = new HashMap<String, URLPatternType>();
			for (FilterMappingType filterMappingType : filterMappingList) {
				if (!filterMappingType.getFilterName().getValue()
						.equals(type.getFilterName().getValue())) {
					continue;
				}
				for (Object obj : filterMappingType
						.getUrlPatternOrServletName()) {
					if (obj instanceof ServletNameType) {
						ServletNameType servletType = (ServletNameType) obj;
						for (SmartServletModel model : servletList.values()) {
							if (model.getConfig().getServletName()
									.equals(servletType.getValue())) {
								patterMap.putAll(model.getPatterns());
							}
						}
					} else if (obj instanceof UrlPatternType) {
						UrlPatternType urlType = (UrlPatternType) obj;
						String url = urlType.getValue();
						if (!validateURLPattern(url)) {
							throw new IllegalArgumentException(
									"invalid url-mapping:" + urlType.getValue());
						}
						// 精确路径匹配
						if (url.startsWith("/") && (url.indexOf("*") < 0)) {
							patterMap.put(url, URLPatternType.AllMatcher);
						}
						// 目录匹配
						else if (url.startsWith("/") && (url.indexOf("*") > 0)) {
							patterMap.put(url.replace("*", ".*"),
									URLPatternType.CatalogMatcher);
						}
						// 扩展名匹配
						else if (url.startsWith("*.")) {
							patterMap.put(
									url.replace(".", "(.)").replace("*", ".*"),
									URLPatternType.ExtensionMatcher);
						} else {
							throw new IllegalArgumentException(
									"invalid url-mapping:" + url);
						}
					} else {
						System.out.println(obj);
					}
				}
			}
			SmartFilterModel model = new SmartFilterModel(patterMap, filter);
			filterModelList.add(model);
		}
	}

	public void destory0() {
		// TODO Auto-generated method stub

	}
}
