/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net> Distributed under the terms of either: - the common
 * development and distribution license (CDDL), v1.0; or - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.accesslog.AccessLogger;
import org.smartboot.socket.protocol.http.accesslog.AccessLoggerProviderFactory;
import org.smartboot.socket.protocol.http.accesslog.PatternType;
import org.smartboot.socket.protocol.http.jndi.JndiManager;
import org.smartboot.socket.protocol.http.loader.FilteringClassLoader;
import org.smartboot.socket.protocol.http.loader.ReloadingClassLoader;
import org.smartboot.socket.protocol.http.loader.WebappClassLoader;
import org.smartboot.socket.protocol.http.servlet.DispatcherSourceType;
import org.smartboot.socket.protocol.http.servlet.ErrorServlet;
import org.smartboot.socket.protocol.http.servlet.InvokerServlet;
import org.smartboot.socket.protocol.http.servlet.StaticResourceServlet;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationHandler;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationRealm;
import org.smartboot.socket.protocol.http.servlet.core.authentication.realm.ArgumentsRealm;
import org.smartboot.socket.protocol.http.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Models the web.xml file's details ... basically just a bunch of configuration details, plus the actual instances of mounted
 * servlets.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WebAppConfiguration.java,v 1.59 2008/10/09 18:44:53 rickknowles Exp $
 */
public class WebAppConfiguration implements ServletContext, Comparator<Object> {
    // private static final String ELEM_DESCRIPTION = "description";

    private static final String ELEM_DISPLAY_NAME = "display-name";
    private static final String ELEM_SERVLET = "servlet";
    private static final String ELEM_SERVLET_MAPPING = "servlet-mapping";
    private static final String ELEM_SERVLET_NAME = "servlet-name";
    private static final String ELEM_FILTER = "filter";
    private static final String ELEM_FILTER_MAPPING = "filter-mapping";
    private static final String ELEM_FILTER_NAME = "filter-name";
    private static final String ELEM_DISPATCHER = "dispatcher";
    private static final String ELEM_URL_PATTERN = "url-pattern";
    private static final String ELEM_WELCOME_FILES = "welcome-file-list";
    private static final String ELEM_WELCOME_FILE = "welcome-file";
    private static final String ELEM_SESSION_CONFIG = "session-config";
    private static final String ELEM_SESSION_TIMEOUT = "session-timeout";
    private static final String ELEM_MIME_MAPPING = "mime-mapping";
    private static final String ELEM_MIME_EXTENSION = "extension";
    private static final String ELEM_MIME_TYPE = "mime-type";
    private static final String ELEM_CONTEXT_PARAM = "context-param";
    private static final String ELEM_PARAM_NAME = "param-name";
    private static final String ELEM_PARAM_VALUE = "param-value";
    private static final String ELEM_LISTENER = "listener";
    private static final String ELEM_LISTENER_CLASS = "listener-class";
    private static final String ELEM_DISTRIBUTABLE = "distributable";
    private static final String ELEM_ERROR_PAGE = "error-page";
    private static final String ELEM_EXCEPTION_TYPE = "exception-type";
    private static final String ELEM_ERROR_CODE = "error-code";
    private static final String ELEM_ERROR_LOCATION = "location";
    private static final String ELEM_SECURITY_CONSTRAINT = "security-constraint";
    private static final String ELEM_LOGIN_CONFIG = "login-config";
    private static final String ELEM_SECURITY_ROLE = "security-role";
    private static final String ELEM_ROLE_NAME = "role-name";
    private static final String ELEM_ENV_ENTRY = "env-entry";
    private static final String ELEM_LOCALE_ENC_MAP_LIST = "locale-encoding-mapping-list";
    private static final String ELEM_LOCALE_ENC_MAPPING = "locale-encoding-mapping";
    private static final String ELEM_LOCALE = "locale";
    private static final String ELEM_ENCODING = "encoding";
    private static final String ELEM_JSP_CONFIG = "jsp-config";
    private static final String ELEM_JSP_PROPERTY_GROUP = "jsp-property-group";
    private static final String DISPATCHER_REQUEST = "REQUEST";
    private static final String DISPATCHER_FORWARD = "FORWARD";
    private static final String DISPATCHER_INCLUDE = "INCLUDE";
    private static final String DISPATCHER_ERROR = "ERROR";
    private static final String JSP_SERVLET_MAPPING = "*.jsp";
    private static final String JSPX_SERVLET_MAPPING = "*.jspx";
    private static final String JSP_SERVLET_LOG_LEVEL = "WARNING";
    private static final String INVOKER_SERVLET_NAME = "invoker";
    private static final String DEFAULT_INVOKER_PREFIX = "/servlet/";
    private static final String DEFAULT_SERVLET_NAME = "default";
    private static final String ERROR_SERVLET_NAME = "winstoneErrorServlet";
    private static final String WEB_INF = "WEB-INF";
    private static final String CLASSES = "classes/";
    private static final String LIB = "lib";
    protected static Logger logger = LogManager.getLogger(WebAppConfiguration.class);
    private final HostConfiguration ownerHostConfig;
    private final String webRoot;
    private final String prefix;
    private final String contextName;
    private final Map<String, Object> attributes;
    private final Map<String, String> initParameters;
    private final Map<String, WinstoneSession> sessions;
    private final Map<String, ServletConfiguration> servletInstances;
    private final Map<String, FilterConfiguration> filterInstances;
    private final ServletContextAttributeListener contextAttributeListeners[];
    private final ServletRequestListener requestListeners[];
    private final ServletRequestAttributeListener requestAttributeListeners[];
    private final HttpSessionActivationListener sessionActivationListeners[];
    private final HttpSessionAttributeListener sessionAttributeListeners[];
    private final HttpSessionListener sessionListeners[];
    private final Map<String, String> exactServletMatchMounts;
    private final Mapping patternMatches[];
    private final Mapping filterPatternsRequest[];
    private final Mapping filterPatternsForward[];
    private final Mapping filterPatternsInclude[];
    private final Mapping filterPatternsError[];
    private final String welcomeFiles[];
    private final Class<?>[] errorPagesByExceptionKeysSorted;
    private final Map<Class<?>, String> errorPagesByException;
    private final Map<String, String> errorPagesByCode;
    private final Map<String, String> localeEncodingMap;
    private final Map<String, FilterConfiguration[]> filterMatchCache;
    private final boolean useSavedSessions;
    private ClassLoader loader;
    private String displayName;
    private WebAppJNDIManager webAppJNDIManager;
    private FileNameMap mimeTypes;
    private ServletContextListener contextListeners[];
    /**
     * 容器启动异常记录
     */
    private Throwable contextStartupError;
    private AuthenticationHandler authenticationHandler;
    private AuthenticationRealm authenticationRealm;
    private Integer sessionTimeout;
    private String defaultServletName;
    private String errorServletName;
    // private JNDIManager jndiManager;
    private AccessLogger accessLogger;

    /**
     * Constructor. This parses the xml and sets up for basic routing
     */
    public WebAppConfiguration(final HostConfiguration ownerHostConfig, final JndiManager jndiManager,
                               final String webRoot, final String prefix, final Map<String, String> startupArgs,
                               final Node elm, final String contextName) {
        this.ownerHostConfig = ownerHostConfig;
        this.webRoot = webRoot;
        if (!prefix.equals("") && !prefix.startsWith("/")) {
            WebAppConfiguration.logger.warn("WARNING: Added missing leading slash to prefix: {}", prefix);
            this.prefix = "/" + prefix;
        } else {
            this.prefix = prefix;
        }
        this.contextName = contextName;

        final List<File> localLoaderClassPathFiles = new ArrayList<File>();

        // not-share-server-classpath
        ClassLoader classLoader = null;
        boolean useServerClassPath = StringUtils.booleanArg(startupArgs, "useServerClassPath", Boolean.TRUE);
        if (useServerClassPath) {
            String filterClassPath = StringUtils.stringArg(startupArgs, "filterClassPath", "org.apache.log4j,org.slf4j");
            StringTokenizer tokenizer = new StringTokenizer(filterClassPath, ",");
            String[] filters = new String[tokenizer.countTokens()];
            int i = 0;
            while (tokenizer.hasMoreTokens()) {
                filters[i] = tokenizer.nextToken();
                i++;
            }
            classLoader = new FilteringClassLoader(Thread.currentThread().getContextClassLoader(), filters);
        } else {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        loader = buildWebAppClassLoader(startupArgs, classLoader, webRoot, localLoaderClassPathFiles);

        // Build switch values
        boolean useJasper = StringUtils.booleanArg(startupArgs, "useJasper", Boolean.TRUE);
        boolean useInvoker = StringUtils.booleanArg(startupArgs, "useInvoker", Boolean.FALSE);
        final boolean useJNDI = StringUtils.booleanArg(startupArgs, "useJNDI", Boolean.FALSE);
        useSavedSessions = WebAppConfiguration.useSavedSessions(startupArgs);

        // Check jasper is available - simple tests
        if (useJasper) {
            try {
                Class.forName(WinstoneConstant.JAVAX_JSP_FACTORY, Boolean.TRUE, Thread.currentThread().getContextClassLoader());
                Class.forName(WinstoneConstant.JSP_SERVLET_CLASS, Boolean.TRUE, loader);
            } catch (final Throwable err) {
                if (StringUtils.booleanArg(startupArgs, "useJasper", Boolean.FALSE)) {
                    WebAppConfiguration.logger
                            .warn("WARNING: Jasper servlet not found - disabling JSP support. Do you have all \nthe jasper libraries in the lib folder ?");
                    WebAppConfiguration.logger.debug("Error loading Jasper JSP compilation servlet");
                }
                useJasper = Boolean.FALSE;
            }
        }
        if (useInvoker) {
            try {
                Class.forName(InvokerServlet.class.getName(), Boolean.FALSE, loader);
            } catch (final Throwable err) {
                WebAppConfiguration.logger.warn("WARNING: Invoker servlet not found - disabling invoker support.");
                useInvoker = Boolean.FALSE;
            }
        }

        attributes = new HashMap<String, Object>();
        initParameters = new HashMap<String, String>();
        sessions = new HashMap<String, WinstoneSession>();

        servletInstances = new HashMap<String, ServletConfiguration>();
        filterInstances = new HashMap<String, FilterConfiguration>();
        filterMatchCache = new HashMap<String, FilterConfiguration[]>();

        final List<ServletContextAttributeListener> contextAttributeListeners = new ArrayList<ServletContextAttributeListener>();
        final List<ServletContextListener> contextListeners = new ArrayList<ServletContextListener>();
        final List<ServletRequestListener> requestListeners = new ArrayList<ServletRequestListener>();
        final List<ServletRequestAttributeListener> requestAttributeListeners = new ArrayList<ServletRequestAttributeListener>();
        final List<HttpSessionActivationListener> sessionActivationListeners = new ArrayList<HttpSessionActivationListener>();
        final List<HttpSessionAttributeListener> sessionAttributeListeners = new ArrayList<HttpSessionAttributeListener>();
        final List<HttpSessionListener> sessionListeners = new ArrayList<HttpSessionListener>();

        errorPagesByException = new HashMap<Class<?>, String>();
        errorPagesByCode = new HashMap<String, String>();
        boolean distributable = Boolean.FALSE;

        exactServletMatchMounts = new HashMap<String, String>();
        final List<Mapping> localFolderPatterns = new ArrayList<Mapping>();
        final List<Mapping> localExtensionPatterns = new ArrayList<Mapping>();

        final List<Mapping> lfpRequest = new ArrayList<Mapping>();
        final List<Mapping> lfpForward = new ArrayList<Mapping>();
        final List<Mapping> lfpInclude = new ArrayList<Mapping>();
        final List<Mapping> lfpError = new ArrayList<Mapping>();

        final List<String> localWelcomeFiles = new ArrayList<String>();
        final List<ServletConfiguration> startupServlets = new ArrayList<ServletConfiguration>();

        final Set<String> rolesAllowed = new HashSet<String>();
        final List<Node> constraintNodes = new ArrayList<Node>();
        final List<Node> envEntryNodes = new ArrayList<Node>();
        final List<Class<?>> localErrorPagesByExceptionList = new ArrayList<Class<?>>();

        Node loginConfigNode = null;

        // Add the class loader as an implicit context listener if it implements
        // the interface
        addListenerInstance(loader, contextAttributeListeners, contextListeners, requestAttributeListeners, requestListeners,
                sessionActivationListeners, sessionAttributeListeners, sessionListeners);

        localeEncodingMap = new HashMap<String, String>();
        final String encodingMapSet = "en_US=8859_1;en=8859_1;ja=SJIS";
        final StringTokenizer st = new StringTokenizer(encodingMapSet, ";");
        for (; st.hasMoreTokens(); ) {
            final String token = st.nextToken();
            final int delimPos = token.indexOf("=");
            if (delimPos == -1) {
                continue;
            }
            localeEncodingMap.put(token.substring(0, delimPos), token.substring(delimPos + 1));
        }

        // init jsp mappings set
        final List<String> jspMappings = new ArrayList<String>();
        jspMappings.add(WebAppConfiguration.JSP_SERVLET_MAPPING);
        jspMappings.add(WebAppConfiguration.JSPX_SERVLET_MAPPING);

        // Add required context atttributes
        final String userName = System.getProperty("user.name", "anyone");
        final File tmpDir = new File(new File(new File(new File(System.getProperty("java.io.tmpdir"), userName), "winstone.tmp"),
                ownerHostConfig.getHostname()), contextName);
        tmpDir.mkdirs();
        attributes.put("javax.servlet.context.tempdir", tmpDir);

        // Parse the web.xml file
        if (elm != null) {
            final NodeList children = elm.getChildNodes();
            Map<String, String> webApplicationMimeType = null;

            for (int n = 0; n < children.getLength(); n++) {
                final Node child = children.item(n);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                final String nodeName = child.getNodeName();

                if (nodeName.equals(WebAppConfiguration.ELEM_DISPLAY_NAME)) {
                    displayName = WebAppConfiguration.getTextFromNode(child);
                } else if (nodeName.equals(WebAppConfiguration.ELEM_DISTRIBUTABLE)) {
                    distributable = Boolean.TRUE;
                } else if (nodeName.equals(WebAppConfiguration.ELEM_SECURITY_CONSTRAINT)) {
                    constraintNodes.add(child);
                } else if (nodeName.equals(WebAppConfiguration.ELEM_ENV_ENTRY)) {
                    envEntryNodes.add(child);
                } else if (nodeName.equals(WebAppConfiguration.ELEM_LOGIN_CONFIG)) {
                    loginConfigNode = child;
                } // Session config elements
                else if (nodeName.equals(WebAppConfiguration.ELEM_SESSION_CONFIG)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        final Node timeoutElm = child.getChildNodes().item(m);
                        if (timeoutElm.getNodeType() == Node.ELEMENT_NODE
                                && timeoutElm.getNodeName().equals(WebAppConfiguration.ELEM_SESSION_TIMEOUT)) {
                            final String timeoutStr = WebAppConfiguration.getTextFromNode(timeoutElm);
                            if (!timeoutStr.equals("")) {
                                sessionTimeout = Integer.valueOf(timeoutStr);
                            }
                        }
                    }
                } // Construct the security roles
                else if (child.getNodeName().equals(WebAppConfiguration.ELEM_SECURITY_ROLE)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        final Node roleElm = child.getChildNodes().item(m);
                        if (roleElm.getNodeType() == Node.ELEMENT_NODE
                                && roleElm.getNodeName().equals(WebAppConfiguration.ELEM_ROLE_NAME)) {
                            rolesAllowed.add(WebAppConfiguration.getTextFromNode(roleElm));
                        }
                    }
                } // Construct the servlet instances
                else if (nodeName.equals(WebAppConfiguration.ELEM_SERVLET)) {
                    final ServletConfiguration instance = new ServletConfiguration(this, child);
                    servletInstances.put(instance.getServletName(), instance);
                    if (instance.getLoadOnStartup() >= 0) {
                        startupServlets.add(instance);
                    }
                } // Construct the servlet instances
                else if (nodeName.equals(WebAppConfiguration.ELEM_FILTER)) {
                    try {
                        final FilterConfiguration instance = new FilterConfiguration(this, loader, child);
                        filterInstances.put(instance.getFilterName(), instance);
                    } catch (ServletException e) {
                        e.printStackTrace();
                    }

                } // Construct the servlet instances
                else if (nodeName.equals(WebAppConfiguration.ELEM_LISTENER)) {
                    String listenerClass = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        final Node listenerElm = child.getChildNodes().item(m);
                        if (listenerElm.getNodeType() == Node.ELEMENT_NODE
                                && listenerElm.getNodeName().equals(WebAppConfiguration.ELEM_LISTENER_CLASS)) {
                            listenerClass = WebAppConfiguration.getTextFromNode(listenerElm);
                        }
                    }
                    if (listenerClass != null) {
                        try {
                            final Class<?> listener = Class.forName(listenerClass, Boolean.TRUE, loader);
                            final Object listenerInstance = listener.newInstance();
                            addListenerInstance(listenerInstance, contextAttributeListeners, contextListeners,
                                    requestAttributeListeners, requestListeners, sessionActivationListeners,
                                    sessionAttributeListeners, sessionListeners);
                            WebAppConfiguration.logger.debug("Adding web application listener: {}", listenerClass);
                        } catch (final Throwable err) {
                            WebAppConfiguration.logger.warn("Error instantiating listener class:  " + listenerClass, err);
                        }
                    }
                } // Process the servlet mappings
                else if (nodeName.equals(WebAppConfiguration.ELEM_SERVLET_MAPPING)) {
                    String name = null;
                    final List<String> mappings = new ArrayList<String>();

                    // Parse the element and extract
                    final NodeList mappingChildren = child.getChildNodes();
                    for (int k = 0; k < mappingChildren.getLength(); k++) {
                        final Node mapChild = mappingChildren.item(k);
                        if (mapChild.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        final String mapNodeName = mapChild.getNodeName();
                        if (mapNodeName.equals(WebAppConfiguration.ELEM_SERVLET_NAME)) {
                            name = WebAppConfiguration.getTextFromNode(mapChild);
                        } else if (mapNodeName.equals(WebAppConfiguration.ELEM_URL_PATTERN)) {
                            mappings.add(WebAppConfiguration.getTextFromNode(mapChild));
                        }
                    }
                    for (final Iterator<String> i = mappings.iterator(); i.hasNext(); ) {
                        processMapping(name, i.next(), exactServletMatchMounts, localFolderPatterns, localExtensionPatterns);
                    }
                } // Process the filter mappings
                else if (nodeName.equals(WebAppConfiguration.ELEM_FILTER_MAPPING)) {
                    String filterName = null;
                    final List<String> mappings = new ArrayList<String>();
                    boolean onRequest = Boolean.FALSE;
                    boolean onForward = Boolean.FALSE;
                    boolean onInclude = Boolean.FALSE;
                    boolean onError = Boolean.FALSE;

                    // Parse the element and extract
                    for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                        final Node mapChild = child.getChildNodes().item(k);
                        if (mapChild.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        final String mapNodeName = mapChild.getNodeName();
                        if (mapNodeName.equals(WebAppConfiguration.ELEM_FILTER_NAME)) {
                            filterName = WebAppConfiguration.getTextFromNode(mapChild);
                        } else if (mapNodeName.equals(WebAppConfiguration.ELEM_SERVLET_NAME)) {
                            mappings.add("srv:" + WebAppConfiguration.getTextFromNode(mapChild));
                        } else if (mapNodeName.equals(WebAppConfiguration.ELEM_URL_PATTERN)) {
                            mappings.add("url:" + WebAppConfiguration.getTextFromNode(mapChild));
                        } else if (mapNodeName.equals(WebAppConfiguration.ELEM_DISPATCHER)) {
                            final String dispatcherValue = WebAppConfiguration.getTextFromNode(mapChild);
                            if (dispatcherValue.equals(WebAppConfiguration.DISPATCHER_REQUEST)) {
                                onRequest = Boolean.TRUE;
                            } else if (dispatcherValue.equals(WebAppConfiguration.DISPATCHER_FORWARD)) {
                                onForward = Boolean.TRUE;
                            } else if (dispatcherValue.equals(WebAppConfiguration.DISPATCHER_INCLUDE)) {
                                onInclude = Boolean.TRUE;
                            } else if (dispatcherValue.equals(WebAppConfiguration.DISPATCHER_ERROR)) {
                                onError = Boolean.TRUE;
                            }
                        }
                    }
                    if (!onRequest && !onInclude && !onForward && !onError) {
                        onRequest = Boolean.TRUE;
                    }
                    if (mappings.isEmpty()) {
                        throw new WinstoneException("Error in filter mapping - no pattern and no servlet name for filter "
                                + filterName);
                    }

                    for (final Iterator<String> i = mappings.iterator(); i.hasNext(); ) {
                        final String item = i.next();
                        Mapping mapping = null;
                        try {
                            if (item.startsWith("srv:")) {
                                mapping = Mapping.createFromLink(filterName, item.substring(4));
                            } else {
                                mapping = Mapping.createFromURL(filterName, item.substring(4));
                            }
                            if (onRequest) {
                                lfpRequest.add(mapping);
                            }
                            if (onForward) {
                                lfpForward.add(mapping);
                            }
                            if (onInclude) {
                                lfpInclude.add(mapping);
                            }
                            if (onError) {
                                lfpError.add(mapping);
                            }
                        } catch (final WinstoneException err) {
                            WebAppConfiguration.logger.warn("Error processing URL mapping: {}", err.getMessage());
                        }
                    }
                } // Process the list of welcome files
                else if (nodeName.equals(WebAppConfiguration.ELEM_WELCOME_FILES)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        final Node welcomeFile = child.getChildNodes().item(m);
                        if (welcomeFile.getNodeType() == Node.ELEMENT_NODE
                                && welcomeFile.getNodeName().equals(WebAppConfiguration.ELEM_WELCOME_FILE)) {
                            final String welcomeStr = WebAppConfiguration.getTextFromNode(welcomeFile);
                            if (!welcomeStr.equals("")) {
                                localWelcomeFiles.add(welcomeStr);
                            }
                        }
                    }
                } // Process the error pages
                else if (nodeName.equals(WebAppConfiguration.ELEM_ERROR_PAGE)) {
                    String code = null;
                    String exception = null;
                    String location = null;

                    // Parse the element and extract
                    for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                        final Node errorChild = child.getChildNodes().item(k);
                        if (errorChild.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        final String errorChildName = errorChild.getNodeName();
                        if (errorChildName.equals(WebAppConfiguration.ELEM_ERROR_CODE)) {
                            code = WebAppConfiguration.getTextFromNode(errorChild);
                        } else if (errorChildName.equals(WebAppConfiguration.ELEM_EXCEPTION_TYPE)) {
                            exception = WebAppConfiguration.getTextFromNode(errorChild);
                        } else if (errorChildName.equals(WebAppConfiguration.ELEM_ERROR_LOCATION)) {
                            location = WebAppConfiguration.getTextFromNode(errorChild);
                        }
                    }
                    if (code != null && location != null) {
                        errorPagesByCode.put(code.trim(), location.trim());
                    }
                    if (exception != null && location != null) {
                        try {
                            final Class<?> exceptionClass = Class.forName(exception.trim(), Boolean.FALSE, loader);
                            localErrorPagesByExceptionList.add(exceptionClass);
                            errorPagesByException.put(exceptionClass, location.trim());
                        } catch (final ClassNotFoundException err) {
                            WebAppConfiguration.logger.error("Exception {} not found in classpath", exception);
                        }
                    }
                } // Process the list of welcome files
                else if (nodeName.equals(WebAppConfiguration.ELEM_MIME_MAPPING)) {
                    String extension = null;
                    String mimeType = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        final Node mimeTypeNode = child.getChildNodes().item(m);
                        if (mimeTypeNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        } else if (mimeTypeNode.getNodeName().equals(WebAppConfiguration.ELEM_MIME_EXTENSION)) {
                            extension = WebAppConfiguration.getTextFromNode(mimeTypeNode);
                        } else if (mimeTypeNode.getNodeName().equals(WebAppConfiguration.ELEM_MIME_TYPE)) {
                            mimeType = WebAppConfiguration.getTextFromNode(mimeTypeNode);
                        }
                    }
                    if (extension != null && mimeType != null) {
                        if (webApplicationMimeType == null) {
                            webApplicationMimeType = new HashMap<String, String>();
                        }
                        webApplicationMimeType.put(extension.toLowerCase(), mimeType);
                    } else {
                        WebAppConfiguration.logger.warn("WebAppConfig: Ignoring invalid mime mapping: extension={} mimeType={}",
                                extension, mimeType);
                    }
                } // Process the list of welcome files
                else if (nodeName.equals(WebAppConfiguration.ELEM_CONTEXT_PARAM)) {
                    String name = null;
                    String value = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        final Node contextParamNode = child.getChildNodes().item(m);
                        if (contextParamNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        } else if (contextParamNode.getNodeName().equals(WebAppConfiguration.ELEM_PARAM_NAME)) {
                            name = WebAppConfiguration.getTextFromNode(contextParamNode);
                        } else if (contextParamNode.getNodeName().equals(WebAppConfiguration.ELEM_PARAM_VALUE)) {
                            value = WebAppConfiguration.getTextFromNode(contextParamNode);
                        }
                    }
                    if (name != null && value != null) {
                        initParameters.put(name, value);
                    } else {
                        WebAppConfiguration.logger
                                .warn("WebAppConfig: Ignoring invalid init parameter: name={} value={}", name, value);
                    }
                } // Process locale encoding mapping elements
                else if (nodeName.equals(WebAppConfiguration.ELEM_LOCALE_ENC_MAP_LIST)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        final Node mappingNode = child.getChildNodes().item(m);
                        if (mappingNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        } else if (mappingNode.getNodeName().equals(WebAppConfiguration.ELEM_LOCALE_ENC_MAPPING)) {
                            String localeName = "";
                            String encoding = "";
                            for (int l = 0; l < mappingNode.getChildNodes().getLength(); l++) {
                                final Node mappingChildNode = mappingNode.getChildNodes().item(l);
                                if (mappingChildNode.getNodeType() != Node.ELEMENT_NODE) {
                                    continue;
                                } else if (mappingChildNode.getNodeName().equals(WebAppConfiguration.ELEM_LOCALE)) {
                                    localeName = WebAppConfiguration.getTextFromNode(mappingChildNode);
                                } else if (mappingChildNode.getNodeName().equals(WebAppConfiguration.ELEM_ENCODING)) {
                                    encoding = WebAppConfiguration.getTextFromNode(mappingChildNode);
                                }
                            }
                            if (!encoding.equals("") && !localeName.equals("")) {
                                localeEncodingMap.put(localeName, encoding);
                            }
                        }
                    }
                } // Record the url mappings for jsp files if set
                else if (nodeName.equals(WebAppConfiguration.ELEM_JSP_CONFIG)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        final Node propertyGroupNode = child.getChildNodes().item(m);
                        if (propertyGroupNode.getNodeType() == Node.ELEMENT_NODE
                                && propertyGroupNode.getNodeName().equals(WebAppConfiguration.ELEM_JSP_PROPERTY_GROUP)) {
                            for (int l = 0; l < propertyGroupNode.getChildNodes().getLength(); l++) {
                                final Node urlPatternNode = propertyGroupNode.getChildNodes().item(l);
                                if (urlPatternNode.getNodeType() == Node.ELEMENT_NODE
                                        && urlPatternNode.getNodeName().equals(WebAppConfiguration.ELEM_URL_PATTERN)) {
                                    final String jm = WebAppConfiguration.getTextFromNode(urlPatternNode);
                                    if (!jm.equals("")) {
                                        jspMappings.add(jm);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            /** Build a specific mime type */
            if (webApplicationMimeType != null) {
                // build mime types with default and those find in web.xml
                mimeTypes = MimeTypes.getInstance(webApplicationMimeType);
            }
        }
        /** Build a default mime type if necessary */
        if (mimeTypes == null) {
            mimeTypes = MimeTypes.getInstance();
        }


        // Build the login/security role instance
        if (!constraintNodes.isEmpty() && loginConfigNode != null) {
            String authMethod = null;
            for (int n = 0; n < loginConfigNode.getChildNodes().getLength(); n++) {
                if (loginConfigNode.getChildNodes().item(n).getNodeName().equals("auth-method")) {
                    authMethod = WebAppConfiguration.getTextFromNode(loginConfigNode.getChildNodes().item(n));
                }
            }
            // Load the appropriate auth class
            if (authMethod == null) {
                authMethod = "BASIC";
            } else {
                authMethod = StringUtils.replace(authMethod, "-", "");
            }
            final String realmClassName = StringUtils
                    .stringArg(startupArgs, "realmClassName", ArgumentsRealm.class.getCanonicalName()).trim();
            final String authClassName = "net.winstone.core.authentication." + authMethod.substring(0, 1).toUpperCase()
                    + authMethod.substring(1).toLowerCase() + "AuthenticationHandler";
            try {
                // Build the realm
                final Class<?> realmClass = Class.forName(realmClassName);
                final Constructor<?> realmConstr = realmClass.getConstructor(new Class[]{Set.class, Map.class});
                authenticationRealm = (AuthenticationRealm) realmConstr.newInstance(new Object[]{rolesAllowed, startupArgs});

                // Build the authentication handler
                final Class<?> authClass = Class.forName(authClassName);
                final Constructor<?> authConstr = authClass.getConstructor(new Class[]{Node.class, List.class, Set.class,
                        AuthenticationRealm.class});
                authenticationHandler = (AuthenticationHandler) authConstr.newInstance(new Object[]{loginConfigNode,
                        constraintNodes, rolesAllowed, authenticationRealm});
            } catch (final ClassNotFoundException err) {
                WebAppConfiguration.logger.error("Authentication disabled - can't load authentication handler for {} authentication",
                        authMethod);
            } catch (final Throwable err) {
                WebAppConfiguration.logger.error("Authentication disabled - couldn't load authentication handler: " + authClassName
                        + " or realm: " + realmClassName, err);
            }
        } else if (!StringUtils.stringArg(startupArgs, "realmClassName", "").trim().equals("")) {
            WebAppConfiguration.logger
                    .warn("WARNING: Realm configuration ignored, because there are no roles defined in the web.xml ");
        }

        // Instantiate the JNDI manager
        if (useJNDI) {
            // creation du context
            webAppJNDIManager = new WebAppJNDIManager(jndiManager, envEntryNodes, loader);
        } else {
            webAppJNDIManager = null;
        }

        final String loggerClassName = StringUtils.stringArg(startupArgs, "accessLoggerClassName", "").trim();
        if (!loggerClassName.equals("")) {
            try {
                accessLogger = AccessLoggerProviderFactory.getAccessLogger(getOwnerHostname(), getContextName(),
                        PatternType.fromName(StringUtils.stringArg(startupArgs, "simpleAccessLogger.format", "combined")),
                        StringUtils.stringArg(startupArgs, "simpleAccessLogger.file", "logs/###host###/###webapp###_access.log"));
            } catch (final Throwable err) {
                WebAppConfiguration.logger.error("Error instantiating access logger class: " + loggerClassName, err);
            }
        } else {
            WebAppConfiguration.logger.info("Access logging disabled - no logger class defined");
        }

        // Add the default index.html welcomeFile if none are supplied
        if (localWelcomeFiles.isEmpty()) {
            if (useJasper) {
                localWelcomeFiles.add("index.jsp");
            }
            localWelcomeFiles.add("index.html");
        }

        // Put the name filters after the url filters, then convert to string
        // arrays
        filterPatternsRequest = lfpRequest.toArray(new Mapping[0]);
        filterPatternsForward = lfpForward.toArray(new Mapping[0]);
        filterPatternsInclude = lfpInclude.toArray(new Mapping[0]);
        filterPatternsError = lfpError.toArray(new Mapping[0]);
        // no sort on filter (@see issue 18)

        welcomeFiles = localWelcomeFiles.toArray(new String[0]);
        errorPagesByExceptionKeysSorted = localErrorPagesByExceptionList.toArray(new Class[0]);
        Arrays.sort(errorPagesByExceptionKeysSorted, this);

        // Put the listeners into their arrays
        this.contextAttributeListeners = contextAttributeListeners.toArray(new ServletContextAttributeListener[0]);
        this.contextListeners = contextListeners.toArray(new ServletContextListener[0]);
        this.requestListeners = requestListeners.toArray(new ServletRequestListener[0]);
        this.requestAttributeListeners = requestAttributeListeners.toArray(new ServletRequestAttributeListener[0]);
        this.sessionActivationListeners = sessionActivationListeners.toArray(new HttpSessionActivationListener[0]);
        this.sessionAttributeListeners = sessionAttributeListeners.toArray(new HttpSessionAttributeListener[0]);
        this.sessionListeners = sessionListeners.toArray(new HttpSessionListener[0]);

        // If we haven't explicitly mapped the default servlet, map it here
        if (defaultServletName == null) {
            defaultServletName = WebAppConfiguration.DEFAULT_SERVLET_NAME;
        }
        if (errorServletName == null) {
            errorServletName = WebAppConfiguration.ERROR_SERVLET_NAME;
        }

        // If we don't have an instance of the default servlet, mount the
        // inbuilt one
        final boolean useDirLists = StringUtils.booleanArg(startupArgs, "directoryListings", Boolean.TRUE);
        final Map<String, String> staticParams = new HashMap<String, String>();
        staticParams.put("webRoot", webRoot);
        staticParams.put("prefix", this.prefix);
        staticParams.put("directoryList", "" + useDirLists);

        if (servletInstances.get(defaultServletName) == null) {
            final ServletConfiguration defaultServlet = new ServletConfiguration(this, defaultServletName,
                    StaticResourceServlet.class.getName(), staticParams, 0);
            servletInstances.put(defaultServletName, defaultServlet);
            startupServlets.add(defaultServlet);
        }
        if (StringUtils.booleanArg(startupArgs, "alwaysMountDefaultServlet", Boolean.TRUE)
                && servletInstances.get(WebAppConfiguration.DEFAULT_SERVLET_NAME) == null) {
            final ServletConfiguration defaultServlet = new ServletConfiguration(this, WebAppConfiguration.DEFAULT_SERVLET_NAME,
                    StaticResourceServlet.class.getName(), staticParams, 0);
            servletInstances.put(WebAppConfiguration.DEFAULT_SERVLET_NAME, defaultServlet);
            startupServlets.add(defaultServlet);
        }

        // If we don't have an instance of the default servlet, mount the
        // inbuilt one
        if (servletInstances.get(errorServletName) == null) {
            final ServletConfiguration errorServlet = new ServletConfiguration(this, errorServletName, ErrorServlet.class.getName(),
                    new HashMap<String, String>(), 0);
            servletInstances.put(errorServletName, errorServlet);
            startupServlets.add(errorServlet);
        }

        // Initialise jasper servlet if requested
        if (useJasper) {
            setAttribute("org.apache.catalina.classloader", loader);
            setAttribute("org.apache.catalina.jsp_classpath", StringUtils.stringArg(startupArgs, "jspClasspath", ""));

            final Map<String, String> jspParams = new HashMap<String, String>();
            WebAppConfiguration.addJspServletParams(jspParams);
            final ServletConfiguration sc = new ServletConfiguration(this, WinstoneConstant.JSP_SERVLET_NAME,
                    WinstoneConstant.JSP_SERVLET_CLASS, jspParams, 3);
            servletInstances.put(WinstoneConstant.JSP_SERVLET_NAME, sc);
            startupServlets.add(sc);
            for (final Iterator<String> mapIt = jspMappings.iterator(); mapIt.hasNext(); ) {
                processMapping(WinstoneConstant.JSP_SERVLET_NAME, mapIt.next(), exactServletMatchMounts, localFolderPatterns,
                        localExtensionPatterns);
            }
        }

        // Initialise invoker servlet if requested
        if (useInvoker) {
            // Get generic options
            final String invokerPrefix = StringUtils.stringArg(startupArgs, "invokerPrefix",
                    WebAppConfiguration.DEFAULT_INVOKER_PREFIX);
            final Map<String, String> invokerParams = new HashMap<String, String>();
            invokerParams.put("prefix", this.prefix);
            invokerParams.put("invokerPrefix", invokerPrefix);
            final ServletConfiguration sc = new ServletConfiguration(this, WebAppConfiguration.INVOKER_SERVLET_NAME,
                    InvokerServlet.class.getName(), invokerParams, 3);
            servletInstances.put(WebAppConfiguration.INVOKER_SERVLET_NAME, sc);
            processMapping(WebAppConfiguration.INVOKER_SERVLET_NAME, invokerPrefix + Mapping.STAR, exactServletMatchMounts,
                    localFolderPatterns, localExtensionPatterns);
        }

        // Sort the folder patterns so the longest paths are first
        localFolderPatterns.addAll(localExtensionPatterns);
        patternMatches = localFolderPatterns.toArray(new Mapping[0]);
        if (patternMatches.length > 0) {
            Arrays.sort(patternMatches, patternMatches[0]);
        }

        // Send init notifies
        try {
            for (int n = 0; n < this.contextListeners.length; n++) {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(loader);
                this.contextListeners[n].contextInitialized(new ServletContextEvent(this));
                Thread.currentThread().setContextClassLoader(cl);
            }
        } catch (final Throwable err) {
            WebAppConfiguration.logger.error("Error during context startup for webapp " + this.contextName, err);
            contextStartupError = err;
        }

        if (contextStartupError == null) {
            // Load sessions if enabled
            if (useSavedSessions) {
                WinstoneSession.loadSessions(this);
            }

            // Initialise all the filters
            for (final Iterator<FilterConfiguration> i = filterInstances.values().iterator(); i.hasNext(); ) {
                final FilterConfiguration config = i.next();
                config.getFilter();
            }

            // Initialise load on startup servlets
            final Object autoStarters[] = startupServlets.toArray();
            Arrays.sort(autoStarters);
            for (int n = 0; n < autoStarters.length; n++) {
                ((ServletConfiguration) autoStarters[n]).ensureInitialization();
            }
        }
    }

    public static String getTextFromNode(final Node node) {
        if (node == null) {
            return null;
        }
        final Node child = node.getFirstChild();
        if (child == null) {
            return "";
        }
        final String textNode = child.getNodeValue();
        if (textNode == null) {
            return "";
        } else {
            return textNode.trim();
        }
    }

    public static boolean useSavedSessions(final Map<String, String> args) {
        return StringUtils.booleanArg(args, "useSavedSessions", Boolean.FALSE);
    }

    public static void addJspServletParams(final Map<String, String> jspParams) {
        jspParams.put("logVerbosityLevel", WebAppConfiguration.JSP_SERVLET_LOG_LEVEL);
        jspParams.put("fork", "Boolean.FALSE");
    }

    /**
     * Build the web-app classloader. This tries to load the preferred classloader first, but if it fails, falls back to a simple
     * URLClassLoader.
     */
    private ClassLoader buildWebAppClassLoader(final Map<String, String> startupArgs, final ClassLoader parentClassLoader,
                                               final String webRoot, final List<File> classPathFileList) {
        final List<URL> urlList = new ArrayList<URL>();

        try {
            // Web-inf folder
            final File webInfFolder = new File(webRoot, WebAppConfiguration.WEB_INF);

            // Classes folder
            final File classesFolder = new File(webInfFolder, WebAppConfiguration.CLASSES);
            if (classesFolder.exists()) {
                WebAppConfiguration.logger.debug("Adding webapp classes folder to classpath");
                final String classesFolderURL = classesFolder.getCanonicalFile().toURI().toURL().toString();
                urlList.add(new URL(classesFolderURL.endsWith("/") ? classesFolderURL : classesFolderURL + "/"));
                classPathFileList.add(classesFolder);
            } else {
                WebAppConfiguration.logger.warn("No webapp classes folder found - {}", classesFolder.toString());
            }

            // Lib folder's jar files
            final File libFolder = new File(webInfFolder, WebAppConfiguration.LIB);
            if (libFolder.exists()) {
                final File jars[] = libFolder.listFiles();
                for (int n = 0; n < jars.length; n++) {
                    final String jarName = jars[n].getName().toLowerCase();
                    if (jarName.endsWith(".jar") || jarName.endsWith(".zip")) {
                        WebAppConfiguration.logger.debug("Adding webapp lib {} to classpath", jars[n].getName());
                        urlList.add(jars[n].toURI().toURL());
                        classPathFileList.add(jars[n]);
                    }
                }
            } else {
                WebAppConfiguration.logger.warn("No webapp lib folder found - {}", libFolder.toString());
            }
        } catch (final MalformedURLException err) {
            throw new WinstoneException("Bad URL in WinstoneClassLoader", err);
        } catch (final IOException err) {
            throw new WinstoneException("IOException in WinstoneClassLoader", err);
        }

        final URL jarURLs[] = urlList.toArray(new URL[urlList.size()]);

        String preferredClassLoader = StringUtils.stringArg(startupArgs, "preferredClassLoader", WebappClassLoader.class.getName());
        if (StringUtils.booleanArg(startupArgs, "useServletReloading", Boolean.FALSE)
                && StringUtils.stringArg(startupArgs, "preferredClassLoader", "").equals("")) {
            preferredClassLoader = ReloadingClassLoader.class.getName();
        }

        // Try to set up the preferred class loader, and if we fail, use the
        // normal one
        ClassLoader outputCL = null;
        if (!preferredClassLoader.equals("")) {
            try {
                final Class<?> preferredCL = Class.forName(preferredClassLoader, Boolean.TRUE, parentClassLoader);
                final Constructor<?> reloadConstr = preferredCL.getConstructor(new Class[]{URL[].class, ClassLoader.class});
                outputCL = (ClassLoader) reloadConstr.newInstance(new Object[]{jarURLs, parentClassLoader});
            } catch (final Throwable err) {
                if (!StringUtils.stringArg(startupArgs, "preferredClassLoader", "").equals("")
                        || !preferredClassLoader.equals(WebappClassLoader.class.getName())) {
                    WebAppConfiguration.logger.error("Erroring setting class loader", err);
                }
            }
        }

        if (outputCL == null) {
            outputCL = new URLClassLoader(jarURLs, parentClassLoader);
        }
        WebAppConfiguration.logger.debug("Using Webapp classloader: {}", outputCL.toString());
        return outputCL;
    }

    private void addListenerInstance(final Object listenerInstance,
                                     final List<ServletContextAttributeListener> contextAttributeListeners,
                                     final List<ServletContextListener> contextListeners,
                                     final List<ServletRequestAttributeListener> requestAttributeListeners,
                                     final List<ServletRequestListener> requestListeners, final List<HttpSessionActivationListener> sessionActivationListeners,
                                     final List<HttpSessionAttributeListener> sessionAttributeListeners, final List<HttpSessionListener> sessionListeners) {
        if (listenerInstance instanceof ServletContextAttributeListener) {
            contextAttributeListeners.add((ServletContextAttributeListener) listenerInstance);
        }
        if (listenerInstance instanceof ServletContextListener) {
            contextListeners.add((ServletContextListener) listenerInstance);
        }
        if (listenerInstance instanceof ServletRequestAttributeListener) {
            requestAttributeListeners.add((ServletRequestAttributeListener) listenerInstance);
        }
        if (listenerInstance instanceof ServletRequestListener) {
            requestListeners.add((ServletRequestListener) listenerInstance);
        }
        if (listenerInstance instanceof HttpSessionActivationListener) {
            sessionActivationListeners.add((HttpSessionActivationListener) listenerInstance);
        }
        if (listenerInstance instanceof HttpSessionAttributeListener) {
            sessionAttributeListeners.add((HttpSessionAttributeListener) listenerInstance);
        }
        if (listenerInstance instanceof HttpSessionListener) {
            sessionListeners.add((HttpSessionListener) listenerInstance);
        }
    }

    @Override
    public String getContextPath() {
        return prefix;
    }

    public String getWebroot() {
        return webRoot;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public AccessLogger getAccessLogger() {
        return accessLogger;
    }

    public Map<String, FilterConfiguration> getFilters() {
        return filterInstances;
    }

    public final String getContextName() {
        return contextName;
    }

    public Class<?>[] getErrorPageExceptions() {
        return errorPagesByExceptionKeysSorted;
    }

    public Map<Class<?>, String> getErrorPagesByException() {
        return errorPagesByException;
    }

    public Map<String, String> getErrorPagesByCode() {
        return errorPagesByCode;
    }

    public Map<String, String> getLocaleEncodingMap() {
        return localeEncodingMap;
    }

    public String[] getWelcomeFiles() {
        return welcomeFiles;
    }

    public Map<String, FilterConfiguration[]> getFilterMatchCache() {
        return filterMatchCache;
    }

    public final String getOwnerHostname() {
        return ownerHostConfig.getHostname();
    }

    public ServletRequestListener[] getRequestListeners() {
        return requestListeners;
    }

    public ServletRequestAttributeListener[] getRequestAttributeListeners() {
        return requestAttributeListeners;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(final Object one, final Object two) {
        if (!(one instanceof Class) || !(two instanceof Class)) {
            throw new IllegalArgumentException("This comparator is only for sorting classes");
        }
        @SuppressWarnings("rawtypes") final Class classOne = (Class) one;
        @SuppressWarnings("rawtypes") final Class classTwo = (Class) two;
        if (classOne.isAssignableFrom(classTwo)) {
            return 1;
        } else if (classTwo.isAssignableFrom(classOne)) {
            return -1;
        } else {
            return 0;
        }
    }

    public String getServletURIFromRequestURI(final String requestURI) {
        if (prefix.equals("")) {
            return requestURI;
        } else if (requestURI.startsWith(prefix)) {
            return requestURI.substring(prefix.length());
        } else {
            throw new WinstoneException("This shouldn't happen, " + "since we aborted earlier if we didn't match");
        }
    }

    /**
     * Iterates through each of the servlets/filters and calls destroy on them
     */
    public void destroy() {
        synchronized (filterMatchCache) {
            filterMatchCache.clear();
        }

        final Collection<FilterConfiguration> filterInstances = new ArrayList<FilterConfiguration>(this.filterInstances.values());

        final ClassLoader cl1 = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        for (final Iterator<FilterConfiguration> i = filterInstances.iterator(); i.hasNext(); ) {
            try {
                FilterConfiguration filterConfiguration = i.next();
                logger.debug("{}: destroy", filterConfiguration.getFilterName());
                filterConfiguration.getFilter().destroy();
            } catch (final Throwable err) {
                WebAppConfiguration.logger.error("Error during servlet context shutdown", err);
            }
        }
        Thread.currentThread().setContextClassLoader(cl1);
        this.filterInstances.clear();

        final Collection<ServletConfiguration> servletInstances = new ArrayList<ServletConfiguration>(this.servletInstances.values());
        for (final Iterator<ServletConfiguration> i = servletInstances.iterator(); i.hasNext(); ) {
            try {
                i.next().destroy();
            } catch (final Throwable err) {
                WebAppConfiguration.logger.error("Error during servlet context shutdown", err);
            }
        }
        this.servletInstances.clear();

        // Drop all sessions
        final Collection<WinstoneSession> sessions = new ArrayList<WinstoneSession>(this.sessions.values());
        for (final Iterator<WinstoneSession> i = sessions.iterator(); i.hasNext(); ) {
            final WinstoneSession session = i.next();
            try {
                if (useSavedSessions) {
                    session.saveToTemp();
                } else {
                    session.invalidate();
                }
            } catch (final Throwable err) {
                WebAppConfiguration.logger.error("Error during servlet context shutdown", err);
            }
        }
        this.sessions.clear();

        // Send destroy notifies - backwards
        for (int n = contextListeners.length - 1; n >= 0; n--) {
            try {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(loader);
                contextListeners[n].contextDestroyed(new ServletContextEvent(this));
                contextListeners[n] = null;
                Thread.currentThread().setContextClassLoader(cl);
            } catch (final Throwable err) {
                WebAppConfiguration.logger.error("Error during servlet context shutdown", err);
            }
        }
        contextListeners = null;

        // Terminate class loader reloading thread if running
        if (loader != null) {
            // already shutdown/handled by the servlet context listeners
            // try {
            // Method methDestroy = this.loader.getClass().getMethod("destroy",
            // new Class[0]);
            // methDestroy.invoke(this.loader, new Object[0]);
            // } catch (Throwable err) {
            // Logger.log(Logger.ERROR, Launcher.RESOURCES,
            // "WebAppConfig.ShutdownError", err);
            // }
            loader = null;
        }

        // Kill JNDI manager if we have one
        if (webAppJNDIManager != null) {
            webAppJNDIManager.destroy();
            webAppJNDIManager = null;
        }

        // Kill JNDI manager if we have one
        if (accessLogger != null) {
            AccessLoggerProviderFactory.destroy(accessLogger);
            accessLogger = null;
        }
    }

    /**
     * Triggered by the admin thread on the reloading class loader. This will cause a full shutdown and reinstantiation of the web app
     * - not real graceful, but you shouldn't have reloading turned on in high load environments.
     */
    public void resetClassLoader() throws IOException {
        ownerHostConfig.reloadWebApp(getContextPath());
    }

    /**
     * Here we process url patterns into the exactMatch and patternMatch lists
     */
    private void processMapping(final String name, final String pattern, final Map<String, String> exactPatterns,
                                final List<Mapping> folderPatterns, final List<Mapping> extensionPatterns) {

        Mapping urlPattern = null;
        try {
            urlPattern = Mapping.createFromURL(name, pattern);
        } catch (final WinstoneException err) {
            WebAppConfiguration.logger.warn("WebAppConfig.ErrorMapURL {}", err.getMessage());
            return;
        }

        // put the pattern in the correct list
        if (urlPattern.getPatternType() == Mapping.EXACT_PATTERN) {
            exactPatterns.put(urlPattern.getUrlPattern(), name);
        } else if (urlPattern.getPatternType() == Mapping.FOLDER_PATTERN) {
            folderPatterns.add(urlPattern);
        } else if (urlPattern.getPatternType() == Mapping.EXTENSION_PATTERN) {
            extensionPatterns.add(urlPattern);
        } else if (urlPattern.getPatternType() == Mapping.DEFAULT_SERVLET) {
            defaultServletName = name;
        } else {
            WebAppConfiguration.logger.warn("WebAppConfig: Invalid pattern mount for {} pattern {} - ignoring", name, pattern);
        }
    }

    /**
     * Execute the pattern match, and try to return a servlet that matches this URL
     */
    private ServletConfiguration urlMatch(final String path, final StringBuilder servletPath, final StringBuilder pathInfo) {
        WebAppConfiguration.logger.debug("URL Match - path: {}", path);

        // Check exact matches first
        final String exact = exactServletMatchMounts.get(path);
        if (exact != null) {
            if (servletInstances.get(exact) != null) {
                servletPath.append(WinstoneRequest.decodePathURLToken(path));
                // pathInfo.append(""); // a hack - empty becomes null later
                return servletInstances.get(exact);
            }
        }

        // Inexact mount check
        for (int n = 0; n < patternMatches.length; n++) {
            final Mapping urlPattern = patternMatches[n];
            if (urlPattern.match(path, servletPath, pathInfo) && servletInstances.get(urlPattern.getMappedTo()) != null) {
                return servletInstances.get(urlPattern.getMappedTo());
            }
        }

        // return default servlet
        // servletPath.append(""); // unneeded
        if (servletInstances.get(defaultServletName) == null) {
            throw new WinstoneException("Matched URL to a servlet that doesn't exist: " + defaultServletName);
        }
        // pathInfo.append(path);
        servletPath.append(WinstoneRequest.decodePathURLToken(path));
        return servletInstances.get(defaultServletName);
    }

    /**
     * Constructs a session instance with the given sessionId
     *
     * @param sessionId The sessionID for the new session
     * @return A valid session object
     */
    public WinstoneSession makeNewSession(final String sessionId) {
        final WinstoneSession ws = new WinstoneSession(sessionId);
        ws.setWebAppConfiguration(this);
        setSessionListeners(ws);
        if (sessionTimeout == null) {
            ws.setMaxInactiveInterval(60 * 60); // 60 mins as the default
        } else if (sessionTimeout.intValue() > 0) {
            ws.setMaxInactiveInterval(sessionTimeout.intValue() * 60);
        } else {
            ws.setMaxInactiveInterval(-1);
        }
        ws.setLastAccessedDate(System.currentTimeMillis());
        ws.sendCreatedNotifies();
        sessions.put(sessionId, ws);
        return ws;
    }

    /**
     * Retrieves the session by id. If the web app is distributable, it asks the other members of the cluster if it doesn't have it
     * itself.
     *
     * @param sessionId The id of the session we want
     * @return A valid session instance
     */
    public WinstoneSession getSessionById(final String sessionId, final boolean localOnly) {
        if (sessionId == null) {
            return null;
        }
        WinstoneSession session = sessions.get(sessionId);
        return session;
    }

    /**
     * Add/Remove the session from the collection
     */
    public void removeSessionById(final String sessionId) {
        sessions.remove(sessionId);
    }

    public void addSession(final String sessionId, final WinstoneSession session) {
        sessions.put(sessionId, session);
    }

    public void invalidateExpiredSessions() {
        final Object allSessions[] = sessions.values().toArray();
        int expiredCount = 0;

        for (int n = 0; n < allSessions.length; n++) {
            final WinstoneSession session = (WinstoneSession) allSessions[n];
            if (/* !session.isNew() && */session.isUnusedByRequests() && session.isExpired()) {
                session.invalidate();
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            WebAppConfiguration.logger.debug("Invalidating {} sessions due to excessive inactivity", expiredCount + "");
        }
    }

    public void setSessionListeners(final WinstoneSession session) {
        session.setSessionActivationListeners(sessionActivationListeners);
        session.setSessionAttributeListeners(sessionAttributeListeners);
        session.setSessionListeners(sessionListeners);
    }

    public void removeServletConfigurationAndMappings(final ServletConfiguration config) {
        servletInstances.remove(config.getServletName());
        // The urlMatch method will only match to non-null mappings, so we don't
        // need
        // to remove anything here
    }

    /***************************************************************************
     * OK ... from here to the end is the interface implementation methods for the servletContext interface.
     **************************************************************************/
    // Application level attributes
    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void removeAttribute(final String name) {
        final Object me = attributes.get(name);
        attributes.remove(name);
        if (me != null) {
            for (int n = 0; n < contextAttributeListeners.length; n++) {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getLoader());
                contextAttributeListeners[n].attributeRemoved(new ServletContextAttributeEvent(this, name, me));
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    @Override
    public final void setAttribute(final String name, final Object object) {
        if (object == null) {
            removeAttribute(name);
        } else {
            final Object me = attributes.get(name);
            attributes.put(name, object);
            if (me != null) {
                for (int n = 0; n < contextAttributeListeners.length; n++) {
                    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(getLoader());
                    contextAttributeListeners[n].attributeReplaced(new ServletContextAttributeEvent(this, name, me));
                    Thread.currentThread().setContextClassLoader(cl);
                }
            } else {
                if (contextAttributeListeners != null) {
                    for (int n = 0; n < contextAttributeListeners.length; n++) {
                        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getLoader());
                        contextAttributeListeners[n].attributeAdded(new ServletContextAttributeEvent(this, name, object));
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                }
            }
        }
    }

    // Application level init parameters
    @Override
    public String getInitParameter(final String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    // Server info
    @Override
    public String getServerInfo() {
        return "ServerVersion";
    }

    @Override
    public int getMajorVersion() {
        return 2;
    }

    @Override
    public int getMinorVersion() {
        return 5;
    }

    // Weird mostly deprecated crap to do with getting servlet instances
    @Override
    public javax.servlet.ServletContext getContext(final String uri) {
        return ownerHostConfig.getWebAppByURI(uri);
    }

    @Override
    public String getServletContextName() {
        return displayName;
    }

    /**
     * Look up the map of mimeType extensions, and return the type that matches
     */
    @Override
    public String getMimeType(final String fileName) {
        return mimeTypes.getContentTypeFor(fileName);
    }

    // Context level log statements
    @Override
    public void log(final String message) {
        if (WebAppConfiguration.logger.isInfoEnabled()) {
            WebAppConfiguration.logger.info(contextName + " " + message);
        }
    }

    @Override
    public void log(final String message, final Throwable throwable) {
        if (WebAppConfiguration.logger.isErrorEnabled()) {
            WebAppConfiguration.logger.error(contextName + " " + message, throwable);
        }
    }

    /**
     * Named dispatcher - this basically gets us a simple exact dispatcher (no url matching, no request attributes and no security)
     */
    @Override
    public javax.servlet.RequestDispatcher getNamedDispatcher(final String name) {
        final ServletConfiguration servlet = servletInstances.get(name);
        if (servlet == null) {
            return null;
        }
        final SimpleRequestDispatcher rd = new SimpleRequestDispatcher(this, servlet, DispatcherSourceType.Named_Dispatcher);
        rd.setForNamedDispatcher(filterPatternsForward, filterPatternsInclude);
        return rd;
    }

    /**
     * Gets a dispatcher, which sets the request attributes, etc on a forward/include. Doesn't execute security though.
     */
    @Override
    public javax.servlet.RequestDispatcher getRequestDispatcher(String uriInsideWebapp) {
        if (uriInsideWebapp == null) {
            return null;
        } else if (!uriInsideWebapp.startsWith("/")) {
            return null;
        }

        // Parse the url for query string, etc
        String queryString = "";
        final int questionPos = uriInsideWebapp.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != uriInsideWebapp.length() - 1) {
                queryString = uriInsideWebapp.substring(questionPos + 1);
            }
            uriInsideWebapp = uriInsideWebapp.substring(0, questionPos);
        }

        // Return the dispatcher
        final StringBuilder servletPath = new StringBuilder();
        final StringBuilder pathInfo = new StringBuilder();
        final ServletConfiguration servlet = urlMatch(uriInsideWebapp, servletPath, pathInfo);
        if (servlet != null) {
            final SimpleRequestDispatcher rd = new SimpleRequestDispatcher(this, servlet, DispatcherSourceType.Request_Dispatcher);
            rd.setForURLDispatcher(servletPath.toString(), pathInfo.toString().equals("") ? null : pathInfo.toString(),
                    queryString, uriInsideWebapp, filterPatternsForward, filterPatternsInclude);
            return rd;
        }
        return null;
    }

    /**
     * Creates the dispatcher that corresponds to a request level dispatch (ie the initial entry point). The difference here is that we
     * need to set up the dispatcher so that on a forward, it executes the security checks and the request filters, while not setting
     * any of the request attributes for a forward. Also, we can't return a null dispatcher in error case - instead we have to return a
     * dispatcher pre-init'd for showing an error page (eg 404). A null dispatcher is interpreted to mean a successful 302 has
     * occurred.
     */
    public SimpleRequestDispatcher getInitialDispatcher(String uriInsideWebapp, final WinstoneRequest request,
                                                        final WinstoneResponse response) throws IOException {
        if (!uriInsideWebapp.equals("") && !uriInsideWebapp.startsWith("/")) {
            return getErrorDispatcherByCode(uriInsideWebapp, HttpServletResponse.SC_BAD_REQUEST, "URI must start with a slash: "
                            + uriInsideWebapp,
                    new IllegalArgumentException("method=" + request.getMethod() + "\nprotocol=" + request.getProtocol()));
        } else if (contextStartupError != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, Boolean.TRUE);
            contextStartupError.printStackTrace(pw);
            return getErrorDispatcherByCode(
                    uriInsideWebapp,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The error below occurred during context initialisation, so no further requests can be \nprocessed:<br><pre>"
                            + sw.toString() + "</pre>", contextStartupError);
        }

        // Parse the url for query string, etc
        String queryString = "";
        final int questionPos = uriInsideWebapp.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != uriInsideWebapp.length() - 1) {
                queryString = uriInsideWebapp.substring(questionPos + 1);
            }
            uriInsideWebapp = uriInsideWebapp.substring(0, questionPos);
        }

        // Return the dispatcher
        final StringBuilder servletPath = new StringBuilder();
        final StringBuilder pathInfo = new StringBuilder();
        final ServletConfiguration servlet = urlMatch(uriInsideWebapp, servletPath, pathInfo);
        if (servlet != null) {
            // If the default servlet was returned, we should check for welcome
            // files
            if (servlet.getServletName().equals(defaultServletName)) {
                // Is path a directory ?
                String directoryPath = servletPath.toString();
                if (directoryPath.endsWith("/")) {
                    directoryPath = directoryPath.substring(0, directoryPath.length() - 1);
                }
                if (directoryPath.startsWith("/")) {
                    directoryPath = directoryPath.substring(1);
                }

                final File res = new File(webRoot, directoryPath);
                if (res.exists() && res.isDirectory() && (request.getMethod().equals("GET") || request.getMethod().equals("HEAD"))) {
                    // Check for the send back with slash case
                    if (!servletPath.toString().endsWith("/")) {
                        WebAppConfiguration.logger.debug("Detected directory with no trailing slash (path={}) - redirecting",
                                servletPath.toString());
                        response.sendRedirect(prefix + servletPath.toString() + pathInfo.toString() + "/"
                                + (queryString.equals("") ? "" : "?" + queryString));
                        return null;
                    }

                    // Check for welcome files
                    WebAppConfiguration.logger.debug("Beginning welcome file match for path: {}",
                            servletPath.toString() + pathInfo.toString());
                    final String welcomeFile = matchWelcomeFiles(servletPath.toString() + pathInfo.toString(), request, queryString);
                    if (welcomeFile != null) {
                        response.sendRedirect(prefix + welcomeFile);
                        // + servletPath.toString()
                        // + pathInfo.toString()
                        // + welcomeFile
                        // + (queryString.equals("") ? "" : "?" + queryString));
                        return null;
                    }
                }
            }

            final SimpleRequestDispatcher rd = new SimpleRequestDispatcher(this, servlet, DispatcherSourceType.Init_Dispatcher);
            rd.setForInitialDispatcher(servletPath.toString(), pathInfo.toString().equals("") ? null : pathInfo.toString(),
                    queryString, uriInsideWebapp, filterPatternsRequest, authenticationHandler);
            return rd;
        }

        // If we are here, return a 404
        return getErrorDispatcherByCode(uriInsideWebapp, HttpServletResponse.SC_NOT_FOUND, "File " + uriInsideWebapp + " not found",
                null);
    }

    /**
     * Gets a dispatcher, set up for error dispatch.
     */
    public SimpleRequestDispatcher getErrorDispatcherByClass(final Throwable exception) {

        // Check for exception class match
        final Class<?> exceptionClasses[] = errorPagesByExceptionKeysSorted;
        Throwable errWrapper = new ServletException(exception);

        while (errWrapper instanceof ServletException) {
            errWrapper = ((ServletException) errWrapper).getRootCause();
            if (errWrapper == null) {
                break;
            }
            for (int n = 0; n < exceptionClasses.length; n++) {
                WebAppConfiguration.logger.debug("Testing error page exception {} against thrown exception {}",
                        errorPagesByExceptionKeysSorted[n].getName(), errWrapper.getClass().getName());
                if (exceptionClasses[n].isInstance(errWrapper)) {
                    final String errorURI = errorPagesByException.get(exceptionClasses[n]);
                    if (errorURI != null) {
                        final SimpleRequestDispatcher rd = buildErrorDispatcher(errorURI,
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, errWrapper);
                        if (rd != null) {
                            return rd;
                        }
                    } else {
                        WebAppConfiguration.logger.warn("Error-page {} not found for exception {}", exceptionClasses[n].getName(),
                                errorPagesByException.get(exceptionClasses[n]));
                    }
                } else {
                    WebAppConfiguration.logger.warn("Exception {} not matched", exceptionClasses[n].getName());
                }
            }
        }

        // Otherwise throw a code error
        Throwable errPassDown = exception;
        while (errPassDown instanceof ServletException && ((ServletException) errPassDown).getRootCause() != null) {
            errPassDown = ((ServletException) errPassDown).getRootCause();
        }
        return getErrorDispatcherByCode(null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, errPassDown);
    }

    public SimpleRequestDispatcher getErrorDispatcherByCode(final String requestURI, final int statusCode,
                                                            final String summaryMessage, final Throwable exception) {
        // Check for status code match
        final String errorURI = getErrorPagesByCode().get("" + statusCode);
        if (errorURI != null) {
            final SimpleRequestDispatcher rd = buildErrorDispatcher(errorURI, statusCode, summaryMessage, exception);
            if (rd != null) {
                return rd;
            }
        }

        // If no dispatcher available, return a dispatcher to the default error
        // formatter
        final ServletConfiguration errorServlet = servletInstances.get(errorServletName);
        if (errorServlet != null) {
            final SimpleRequestDispatcher rd = new SimpleRequestDispatcher(this, errorServlet, DispatcherSourceType.Error_Dispatcher);
            if (rd != null) {
                rd.setForErrorDispatcher(null, null, null, statusCode, summaryMessage, exception, requestURI, filterPatternsError);
                return rd;
            }
        }

        // Otherwise log and return null
        WebAppConfiguration.logger.error("No error servlet available: status code " + statusCode, exception);
        return null;
    }

    /**
     * Build a dispatcher to the error handler if it's available. If it fails, return null.
     */
    private SimpleRequestDispatcher buildErrorDispatcher(String errorURI, final int statusCode, String summaryMessage,
                                                         final Throwable exception) {
        // Parse the url for query string, etc
        String queryString = "";
        final int questionPos = errorURI.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != errorURI.length() - 1) {
                queryString = errorURI.substring(questionPos + 1);
            }
            errorURI = errorURI.substring(0, questionPos);
        }

        // Get the message by recursing if none supplied
        ServletException errIterator = new ServletException(exception);
        while (summaryMessage == null && errIterator != null) {
            summaryMessage = errIterator.getMessage();
            if (errIterator.getRootCause() instanceof ServletException) {
                errIterator = (ServletException) errIterator.getRootCause();
            } else {
                if (summaryMessage == null && errIterator.getCause() != null) {
                    summaryMessage = errIterator.getRootCause().getMessage();
                }
                errIterator = null;
            }
        }

        // Return the dispatcher
        final StringBuilder servletPath = new StringBuilder();
        final StringBuilder pathInfo = new StringBuilder();
        final ServletConfiguration servlet = urlMatch(errorURI, servletPath, pathInfo);
        if (servlet != null) {
            final SimpleRequestDispatcher rd = new SimpleRequestDispatcher(this, servlet, DispatcherSourceType.Error_Dispatcher);
            if (rd != null) {
                rd.setForErrorDispatcher(servletPath.toString(), pathInfo.toString().equals("") ? null : pathInfo.toString(),
                        queryString, statusCode, summaryMessage, exception, errorURI, filterPatternsError);
                return rd;
            }
        }
        return null;
    }

    /**
     * Check if any of the welcome files under this path are available. Returns the name of the file if found, null otherwise. Returns
     * the full internal webapp uri
     */
    private String matchWelcomeFiles(String path, final WinstoneRequest request, final String queryString) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        final String qs = queryString.equals("") ? "" : "?" + queryString;
        for (int n = 0; n < welcomeFiles.length; n++) {
            String welcomeFile = welcomeFiles[n];
            while (welcomeFile.startsWith("/")) {
                welcomeFile = welcomeFile.substring(1);
            }
            welcomeFile = path + welcomeFile;

            final String exact = exactServletMatchMounts.get(welcomeFile);
            if (exact != null) {
                return welcomeFile + qs;
            }

            // Inexact folder mount check - note folder mounts only
            for (int j = 0; j < patternMatches.length; j++) {
                final Mapping urlPattern = patternMatches[j];
                if (urlPattern.getPatternType() == Mapping.FOLDER_PATTERN && urlPattern.match(welcomeFile, null, null)) {
                    return welcomeFile + qs;
                }
            }

            try {
                if (getResource(welcomeFile) != null) {
                    return welcomeFile + qs;
                }
            } catch (final MalformedURLException err) {
            }
        }
        return null;
    }

    // Getting resources via the classloader
    @Override
    public URL getResource(String path) throws MalformedURLException {
        if (path == null) {
            return null;
        } else if (!path.startsWith("/")) {
            throw new MalformedURLException("Bad resource path: path=" + path);
        } else if (!path.equals("/") && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        final File res = new File(webRoot, StringUtils.canonicalPath(path));
        return res != null && res.exists() ? res.toURI().toURL() : null;
    }

    @Override
    public InputStream getResourceAsStream(final String path) {
        try {
            final URL res = getResource(path);
            return res == null ? null : res.openStream();
        } catch (final IOException err) {
            throw new WinstoneException("Error opening resource stream", err);
        }
    }

    @Override
    public String getRealPath(final String path) {
        // Trim the prefix
        if (path == null) {
            return null;
        } else {
            try {
                final File res = new File(webRoot, path);
                if (res.isDirectory()) {
                    return res.getCanonicalPath() + "/";
                } else {
                    return res.getCanonicalPath();
                }
            } catch (final IOException err) {
                return null;
            }
        }
    }

    @Override
    public Set<String> getResourcePaths(final String path) {
        // Trim the prefix
        if (path == null) {
            return null;
        } else if (!path.startsWith("/")) {
            throw new WinstoneException("Bad resource path: path=" + path);
        } else {
            String workingPath = null;
            if (path.equals("/")) {
                workingPath = "";
            } else {
                final boolean lastCharIsSlash = path.charAt(path.length() - 1) == '/';
                workingPath = path.substring(1, path.length() - (lastCharIsSlash ? 1 : 0));
            }
            final File inPath = new File(webRoot, workingPath.equals("") ? "." : workingPath).getAbsoluteFile();
            if (!inPath.exists()) {
                return null;
            } else if (!inPath.isDirectory()) {
                return null;
            }

            // Find all the files in this folder
            final File children[] = inPath.listFiles();
            final Set<String> out = new HashSet<String>();
            for (int n = 0; n < children.length; n++) {
                // Write the entry as subpath + child element
                final String entry = // this.prefix +
                        "/" + (workingPath.length() != 0 ? workingPath + "/" : "") + children[n].getName()
                                + (children[n].isDirectory() ? "/" : "");
                out.add(entry);
            }
            return out;
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public javax.servlet.Servlet getServlet(final String name) {
        return null;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Enumeration getServletNames() {
        return Collections.enumeration(new ArrayList());
    }

    /**
     * @deprecated
     */
    @Deprecated
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Enumeration getServlets() {
        return Collections.enumeration(new ArrayList());
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void log(final Exception exception, final String msg) {
        this.log(msg, exception);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (prefix == null ? 0 : prefix.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return Boolean.TRUE;
        }
        if (obj == null) {
            return Boolean.FALSE;
        }
        if (getClass() != obj.getClass()) {
            return Boolean.FALSE;
        }
        final WebAppConfiguration other = (WebAppConfiguration) obj;
        if (prefix == null) {
            if (other.prefix != null) {
                return Boolean.FALSE;
            }
        } else if (!prefix.equals(other.prefix)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    public String toString() {
        return "WebAppConfiguration [contextPath=" + prefix + "]";
    }
}
