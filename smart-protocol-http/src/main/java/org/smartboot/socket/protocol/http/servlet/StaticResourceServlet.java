/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.WinstoneResourceBundle;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneRequest;
import org.smartboot.socket.protocol.http.util.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Servlet to handle static resources. Simply finds and sends them, or
 * dispatches to the error servlet.<br />
 * Result is templates by init parameters for :
 * <ul>
 * <li><i>oddColour</i> default is #dddddd</li>
 * <li><i>evenColour</i> default is #cbcbcb</li>
 * <li><i>rowTextColour</i> default is #000033</li>
 * <li><i>directoryLabel</i> default is 'directory'</li>
 * <li><i>parentDirLabel</i> default is '(parent directory)'</li>
 * <li><i>noDateLabel</i> default is '-'</li>
 * <li><i>headerColour</i> default is #ffffff</li>
 * <li><i>headerTextColour</i> default is #000033</li>
 * <li><i>labelColour</i> default is #aeaeae</li>
 * <li><i>labelTextColour</i> default is #ffffff"</li>
 * </ul>
 * And by Constant in net.winston.winstone.properties, by:
 * <ul>
 * <li><i>StaticResourceServlet.Body</i> body template</li>
 * <li><i>StaticResourceServlet.Row</i> row template</li>
 * </ul>
 *
 * @author Jerome Guibert
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: StaticResourceServlet.java,v 1.17 2004/12/31 07:21:00
 * rickknowles Exp $
 */
public class StaticResourceServlet extends HttpServlet {

    // final String JSP_FILE = "org.apache.catalina.jsp_file";
    final static String FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
    final static String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
    final static String CACHED_RESOURCE_DATE_HEADER = "If-Modified-Since";
    final static String LAST_MODIFIED_DATE_HEADER = "Last-Modified";
    final static String RANGE_HEADER = "Range";
    final static String ACCEPT_RANGES_HEADER = "Accept-Ranges";
    final static String CONTENT_RANGE_HEADER = "Content-Range";
    private static final long serialVersionUID = 6699448605081774638L;
    protected Logger logger = LogManager.getLogger(getClass());
    private File webRoot;
    private String prefix;
    private boolean directoryList;
    private String serverVersion;
    private String oddColour;
    private String evenColour;
    private String rowTextColour;
    private String directoryLabel;
    private String parentDirLabel;
    private String noDateLabel;
    private String headerColour;
    private String headerTextColour;
    private String labelColour;
    private String labelTextColour;
    private String rowTemplate;
    private String bodyTemplate;

    public static boolean isDescendant(final File parent, final File child, final File commonBase) throws IOException {
        if (child.equals(parent)) {
            return Boolean.TRUE;
        } else {
            // Start by checking canonicals
            final String canonicalParent = parent.getAbsoluteFile().getCanonicalPath();
            final String canonicalChild = child.getAbsoluteFile().getCanonicalPath();
            if (canonicalChild.startsWith(canonicalParent)) {
                return Boolean.TRUE;
            }

            // If canonicals don't match, we're dealing with symlinked files, so
            // if we can
            // build a path from the parent to the child,
            final String childOCValue = StaticResourceServlet.constructOurCanonicalVersion(child, commonBase);
            final String parentOCValue = StaticResourceServlet.constructOurCanonicalVersion(parent, commonBase);
            return childOCValue.startsWith(parentOCValue);
        }
    }

    public static String constructOurCanonicalVersion(File current, final File stopPoint) {
        int backOnes = 0;
        final StringBuilder ourCanonicalVersion = new StringBuilder();
        while ((current != null) && !current.equals(stopPoint)) {
            if (current.getName().equals("..")) {
                backOnes++;
            } else if (current.getName().equals(".")) {
                // skip - do nothing
            } else if (backOnes > 0) {
                backOnes--;
            } else {
                ourCanonicalVersion.insert(0, "/" + current.getName());
            }
            current = current.getParentFile();
        }
        return ourCanonicalVersion.toString();
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        webRoot = new File(config.getInitParameter("webRoot"));
        prefix = config.getInitParameter("prefix");
        final String dirList = config.getInitParameter("directoryList");
        directoryList = (dirList == null) || dirList.equalsIgnoreCase("true") || dirList.equalsIgnoreCase("yes");
        serverVersion = WinstoneResourceBundle.getInstance().getString("ServerVersion");

        oddColour = StringUtils.get(config.getInitParameter("oddColour"), "#dddddd");
        evenColour = StringUtils.get(config.getInitParameter("evenColour"), "#cbcbcb");
        rowTextColour = StringUtils.get(config.getInitParameter("rowTextColour"), "#000033");
        directoryLabel = StringUtils.get(config.getInitParameter("directoryLabel"), "directory");
        parentDirLabel = StringUtils.get(config.getInitParameter("parentDirLabel"), "(parent directory)");
        noDateLabel = StringUtils.get(config.getInitParameter("noDateLabel"), "-");

        headerColour = StringUtils.get(config.getInitParameter("headerColour"), "#ffffff");
        headerTextColour = StringUtils.get(config.getInitParameter("headerTextColour"), "#000033");
        labelColour = StringUtils.get(config.getInitParameter("labelColour"), "#aeaeae");
        labelTextColour = StringUtils.get(config.getInitParameter("labelTextColour"), "#ffffff");

        rowTemplate = WinstoneResourceBundle.getInstance().getString("StaticResourceServlet.Row");
        bodyTemplate = WinstoneResourceBundle.getInstance().getString("StaticResourceServlet.Body");

    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final boolean isInclude = (request.getAttribute(StaticResourceServlet.INCLUDE_SERVLET_PATH) != null);
        final boolean isForward = (request.getAttribute(StaticResourceServlet.FORWARD_SERVLET_PATH) != null);
        String path = null;

        if (isInclude) {
            path = (String) request.getAttribute(StaticResourceServlet.INCLUDE_SERVLET_PATH);
        } else {
            path = request.getServletPath();
        }

        // URL decode path
        path = WinstoneRequest.decodeURLToken(path);

        final long cachedResDate = request.getDateHeader(StaticResourceServlet.CACHED_RESOURCE_DATE_HEADER);
        logger.debug("{}: path={}", getServletConfig().getServletName(), path);

        // Check for the resource
        final File res = path.equals("") ? webRoot : new File(webRoot, path);

        // Send a 404 if not found
        if (!res.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, StringUtils.replaceToken("File {} not found", path));
        } // Check we are below the webroot
        else if (!StaticResourceServlet.isDescendant(webRoot, res, webRoot)) {
            logger.debug("Requested path {} was outside the webroot {}", res.getCanonicalPath(), webRoot.toString());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, StringUtils.replaceToken("Illegal path error - {}", path));
        } // Check we are not below the web-inf
        else if (!isInclude && !isForward && StaticResourceServlet.isDescendant(new File(webRoot, "WEB-INF"), res, webRoot)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, StringUtils.replaceToken("Illegal path error - {}", path));
        } // Check we are not below the meta-inf
        else if (!isInclude && !isForward && StaticResourceServlet.isDescendant(new File(webRoot, "META-INF"), res, webRoot)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, StringUtils.replaceToken("Illegal path error - {}", path));
        } // check for the directory case
        else if (res.isDirectory()) {
            if (path.endsWith("/")) {
                // Try to match each of the welcome files
                // String matchedWelcome = matchWelcomeFiles(path, res);
                // if (matchedWelcome != null)
                // response.sendRedirect(this.prefix + path + matchedWelcome);
                // else
                if (directoryList) {
                    generateDirectoryList(request, response, path);
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to this resource is denied");
                }
            } else {
                response.sendRedirect(prefix + path + "/");
            }
        } // Send a 304 if not modified
        else if (!isInclude && (cachedResDate != -1) && (cachedResDate < ((System.currentTimeMillis() / 1000L) * 1000L)) && (cachedResDate >= ((res.lastModified() / 1000L) * 1000L))) {
            final String mimeType = getServletContext().getMimeType(res.getName().toLowerCase());
            if (mimeType != null) {
                response.setContentType(mimeType);
            }
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setContentLength(0);
            response.flushBuffer();
        } // Write out the resource if not range or is included
        else if ((request.getHeader(StaticResourceServlet.RANGE_HEADER) == null) || isInclude) {
            final String mimeType = getServletContext().getMimeType(res.getName().toLowerCase());
            if (mimeType != null) {
                response.setContentType(mimeType);
            }
            final InputStream resStream = new FileInputStream(res);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength((int) res.length());
            // response.addHeader(ACCEPT_RANGES_HEADER, "bytes");
            response.addDateHeader(StaticResourceServlet.LAST_MODIFIED_DATE_HEADER, res.lastModified());
            OutputStream out = null;
            Writer outWriter = null;
            try {
                out = response.getOutputStream();
            } catch (final IllegalStateException err) {
                outWriter = response.getWriter();
            } catch (final IllegalArgumentException err) {
                outWriter = response.getWriter();
            }
            final byte buffer[] = new byte[4096];
            int read = resStream.read(buffer);
            while (read > 0) {
                if (out != null) {
                    out.write(buffer, 0, read);
                } else {
                    outWriter.write(new String(buffer, 0, read, response.getCharacterEncoding()));
                }
                read = resStream.read(buffer);
            }
            resStream.close();
        } else if (request.getHeader(StaticResourceServlet.RANGE_HEADER).startsWith("bytes=")) {
            final String mimeType = getServletContext().getMimeType(res.getName().toLowerCase());
            if (mimeType != null) {
                response.setContentType(mimeType);
            }
            final InputStream resStream = new FileInputStream(res);

            final List<String> ranges = new ArrayList<String>();
            final StringTokenizer st = new StringTokenizer(request.getHeader(StaticResourceServlet.RANGE_HEADER).substring(6).trim(), ",", Boolean.FALSE);
            int totalSent = 0;
            String rangeText = "";
            while (st.hasMoreTokens()) {
                final String rangeBlock = st.nextToken();
                int start = 0;
                int end = (int) res.length();
                final int delim = rangeBlock.indexOf('-');
                if (delim != 0) {
                    start = Integer.parseInt(rangeBlock.substring(0, delim).trim());
                }
                if (delim != (rangeBlock.length() - 1)) {
                    end = Integer.parseInt(rangeBlock.substring(delim + 1).trim());
                }
                totalSent += (end - start);
                rangeText += "," + start + "-" + end;
                ranges.add(start + "-" + end);
            }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.addHeader(StaticResourceServlet.CONTENT_RANGE_HEADER, "bytes " + rangeText.substring(1) + "/" + res.length());
            response.setContentLength(totalSent);

            response.addHeader(StaticResourceServlet.ACCEPT_RANGES_HEADER, "bytes");
            response.addDateHeader(StaticResourceServlet.LAST_MODIFIED_DATE_HEADER, res.lastModified());
            final OutputStream out = response.getOutputStream();
            int bytesRead = 0;
            for (final Iterator<String> i = ranges.iterator(); i.hasNext(); ) {
                final String rangeBlock = i.next();
                final int delim = rangeBlock.indexOf('-');
                final int start = Integer.parseInt(rangeBlock.substring(0, delim));
                final int end = Integer.parseInt(rangeBlock.substring(delim + 1));
                int read = 0;
                while ((read != -1) && (bytesRead <= res.length())) {
                    read = resStream.read();
                    if ((bytesRead >= start) && (bytesRead < end)) {
                        out.write(read);
                    }
                    bytesRead++;
                }
            }
            resStream.close();
        } else {
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        }
    }

    /**
     * Generate a list of the files in this directory
     */
    private void generateDirectoryList(final HttpServletRequest request, final HttpServletResponse response, final String path) throws ServletException, IOException {
        // Get the file list
        final File dir = path.equals("") ? webRoot : new File(webRoot, path);
        final File children[] = dir.listFiles();
        Arrays.sort(children);

        // Build row content
        final StringWriter rowString = new StringWriter();

        int rowCount = 0;

        // Write the parent dir row
        if (!path.equals("") && !path.equals("/")) {
            rowString.write(StringUtils.replaceToken(rowTemplate, rowTextColour, evenColour, parentDirLabel, "..", noDateLabel, directoryLabel));
            rowCount++;
        }

        // Write the rows for each file
        final DateFormat sdfFileDate = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        for (int n = 0; n < children.length; n++) {
            if (!children[n].getName().equalsIgnoreCase("web-inf") && !children[n].getName().equalsIgnoreCase("meta-inf")) {
                final File file = children[n];
                String date = noDateLabel;
                String size = directoryLabel;
                if (!file.isDirectory()) {
                    size = "" + file.length();
                    date = sdfFileDate.format(new Date(file.lastModified()));
                }
                rowString
                        .write(StringUtils.replaceToken(rowTemplate, rowTextColour, (rowCount % 2) == 0 ? evenColour : oddColour, file.getName() + (file.isDirectory() ? "/" : ""), "./" + file.getName() + (file.isDirectory() ? "/" : ""), date, size));
                rowCount++;
            }
        }

        // Build wrapper body
        final String out = StringUtils.replaceToken(bodyTemplate, headerColour, headerTextColour, labelColour, labelTextColour, new Date().toString(), serverVersion, path.equals("") ? "/" : path, rowString.toString());

        response.setContentLength(out.getBytes().length);
        response.setContentType("text/html");
        final Writer w = response.getWriter();
        w.write(out);
        w.close();
    }
}
