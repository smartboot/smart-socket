/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * The web.xml parsing logic. This is used by more than one launcher, so it's
 * shared from here.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WebXmlParser.java,v 1.9 2006/12/08 04:08:44 rickknowles Exp $
 */
public class WebXmlParser implements EntityResolver, ErrorHandler {

    private final static String SCHEMA_SOURCE_PROPERTY = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    /**
     * Table mapping public doctypes and system ids against local classloader
     * paths. This is used to resolve local entities where possible. Column 0 =
     * public doctype Column 1 = system id Column 2 = local path
     */
    private static final String LOCAL_ENTITY_TABLE[][] = {{"-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN", null, "javax/servlet/resources/web-app_2_2.dtd"},
            {"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN", null, "javax/servlet/resources/web-app_2_3.dtd"}, {null, "http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd", "javax/servlet/resources/web-app_2_4.xsd"},
            {null, "http://java.sun.com/xml/ns/j2ee/web-app_2_5.xsd", "javax/servlet/resources/web-app_2_5.xsd"}, {null, "http://www.w3.org/2001/xml.xsd", "javax/servlet/resources/xml.xsd"},
            {"-//W3C//DTD XMLSCHEMA 200102//EN", null, "javax/servlet/resources/XMLSchema.dtd"}, {null, "http://www.w3.org/2001/datatypes.dtd", "javax/servlet/resources/datatypes.dtd"},
            {null, "http://java.sun.com/xml/ns/j2ee/j2ee_1_4.xsd", "javax/servlet/resources/j2ee_1_4.xsd"}, {null, "http://java.sun.com/xml/ns/j2ee/javaee_5.xsd", "javax/servlet/resources/javaee_5.xsd"},
            {null, "http://java.sun.com/xml/ns/j2ee/jsp_2_0.xsd", "javax/servlet/resources/jsp_2_0.xsd"}, {null, "http://java.sun.com/xml/ns/j2ee/jsp_2_1.xsd", "javax/servlet/resources/jsp_2_1.xsd"},
            {null, "http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_1.xsd", "javax/servlet/resources/j2ee_web_services_client_1_1.xsd"},
            {null, "http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_2.xsd", "javax/servlet/resources/javaee_web_services_client_1_2.xsd"}};
    ;
    protected static Logger logger = LogManager.getLogger(WebXmlParser.class);
    private boolean rethrowValidationExceptions = Boolean.TRUE;

    public WebXmlParser() {
        super();
    }

    /**
     * Get a parsed XML DOM from the given inputstream. Used to process the
     * web.xml application deployment descriptors. Returns null if the parse
     * fails, so the effect is as if there was no web.xml file available.
     */
    public Document parseStreamToXML(final File webXmlFile) {
        final DocumentBuilderFactory factory = getBaseDBF();

        final URL localXSD25 = Thread.currentThread().getContextClassLoader().getResource(WebXmlParser.LOCAL_ENTITY_TABLE[3][2]);
        final URL localXSD24 = Thread.currentThread().getContextClassLoader().getResource(WebXmlParser.LOCAL_ENTITY_TABLE[2][2]);

        // Test for XSD compliance
        try {
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
            if (localXSD25 != null) {
                factory.setAttribute(WebXmlParser.SCHEMA_SOURCE_PROPERTY, localXSD25.toString());
                WebXmlParser.logger.info("Found and enabled the local Servlet 2.5 XSD replacement");
            } else if (localXSD24 != null) {
                factory.setAttribute(WebXmlParser.SCHEMA_SOURCE_PROPERTY, localXSD24.toString());
                WebXmlParser.logger.info("Found and enabled the local Servlet 2.4 XSD replacement");
            } else {
                WebXmlParser.logger.warn("WARNING: The Servlet 2.4/2.5 spec XSD was unavailable inside the winstone classpath. \nWill be retrieved from the web if required (slow)");
            }
        } catch (final Throwable err) {
            // if non-compliant parser, then parse as non-XSD compliant
            WebXmlParser.logger.warn("WARNING: Non-XML-Schema-compliant parser detected. Servlet spec <= 2.3 supported");
            try {
                rethrowValidationExceptions = Boolean.FALSE;
                return parseAsV23Webapp(webXmlFile);
            } catch (final Throwable v23Err) {
                WebXmlParser.logger.error("Error when parsing web.xml file using the v2.2/2.3 servlet specification:", v23Err);
                return null;
            }
        }

        // XSD compliant parser available, so parse as 2.5
        try {
            if (localXSD25 != null) {
                factory.setAttribute(WebXmlParser.SCHEMA_SOURCE_PROPERTY, localXSD25.toString());
            } else {
                factory.setAttribute(WebXmlParser.SCHEMA_SOURCE_PROPERTY, null);
            }
            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(this);
            builder.setErrorHandler(this);
            rethrowValidationExceptions = Boolean.TRUE;
            return builder.parse(webXmlFile);
        } catch (final Throwable errV25) {
            try {
                // Try as 2.4
                if (localXSD24 != null) {
                    factory.setAttribute(WebXmlParser.SCHEMA_SOURCE_PROPERTY, localXSD24.toString());
                } else {
                    factory.setAttribute(WebXmlParser.SCHEMA_SOURCE_PROPERTY, null);
                }
                final DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setEntityResolver(this);
                builder.setErrorHandler(this);
                rethrowValidationExceptions = Boolean.TRUE;
                return builder.parse(webXmlFile);
            } catch (final Throwable errV24) {
                // Try parsing as a v2.3 spec webapp, and if another error
                // happens, report 2.3, 2.4, 2.5
                try {
                    rethrowValidationExceptions = Boolean.FALSE;
                    return parseAsV23Webapp(webXmlFile);
                } catch (final Throwable errV23) {
                    WebXmlParser.logger.error("ERROR: An XSD compliant parser was available, but web.xml parsing failed under both XSD and non-XSD conditions. See below for error reports.");
                    WebXmlParser.logger.error("Error when parsing web.xml file using the v2.5 servlet specification:", errV25);
                    WebXmlParser.logger.error("Error when parsing web.xml file using the v2.4 servlet specification:", errV24);
                    WebXmlParser.logger.error("Error when parsing web.xml file using the v2.2/2.3 servlet specification:", errV23);
                    return null;
                }
            }
        }
    }

    private Document parseAsV23Webapp(final File webXmlFile) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory factory = getBaseDBF();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(this);
        builder.setErrorHandler(this);
        return builder.parse(webXmlFile);
    }

    private DocumentBuilderFactory getBaseDBF() {
        // Use JAXP to create a document builder
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(Boolean.FALSE);
        factory.setValidating(Boolean.TRUE);
        factory.setNamespaceAware(Boolean.TRUE);
        factory.setIgnoringComments(Boolean.TRUE);
        factory.setCoalescing(Boolean.TRUE);
        factory.setIgnoringElementContentWhitespace(Boolean.TRUE);
        return factory;
    }

    /**
     * Implements the EntityResolver interface. This allows us to redirect any
     * requests by the parser for webapp DTDs to local copies. It's faster and
     * it means you can run winstone without being web-connected.
     */
    @Override
    public InputSource resolveEntity(final String publicName, final String url) throws SAXException, IOException {
        WebXmlParser.logger.debug("Resolving entity - public={}, url={}", publicName, url);
        for (int n = 0; n < WebXmlParser.LOCAL_ENTITY_TABLE.length; n++) {
            if (WebXmlParser.LOCAL_ENTITY_TABLE[n][0] != null && publicName != null && publicName.equals(WebXmlParser.LOCAL_ENTITY_TABLE[n][0]) || WebXmlParser.LOCAL_ENTITY_TABLE[n][1] != null && url != null
                    && url.equals(WebXmlParser.LOCAL_ENTITY_TABLE[n][1])) {
                if (Thread.currentThread().getContextClassLoader().getResource(WebXmlParser.LOCAL_ENTITY_TABLE[n][2]) != null) {
                    return getLocalResource(url, WebXmlParser.LOCAL_ENTITY_TABLE[n][2]);
                }
            }
        }
        if (url != null && url.startsWith("jar:")) {
            return getLocalResource(url, url.substring(url.indexOf("!/") + 2));
        } else if (url != null && url.startsWith("file:")) {
            return new InputSource(url);
        } else {
            WebXmlParser.logger.debug("Cannot find local resource for url: {}", url);
            return new InputSource(url);
        }
    }

    private InputSource getLocalResource(final String url, final String local) {
        if (Thread.currentThread().getContextClassLoader().getResource(local) == null) {
            return new InputSource(url);
        }
        final InputSource is = new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(local));
        is.setSystemId(url);
        return is;
    }

    @Override
    public void error(final SAXParseException exception) throws SAXException {
        if (rethrowValidationExceptions) {
            throw exception;
        } else {
            WebXmlParser.logger.warn("XML Error (Line {}): {}", exception.getLineNumber() + "", exception.getMessage());
        }
    }

    @Override
    public void fatalError(final SAXParseException exception) throws SAXException {
        error(exception);
    }

    @Override
    public void warning(final SAXParseException exception) throws SAXException {
        WebXmlParser.logger.warn("XML Error (Line {}): {}", exception.getLineNumber() + "", exception.getMessage());
    }
}
