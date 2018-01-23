/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.accesslog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneRequest;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneResponse;
import org.smartboot.socket.protocol.http.util.DateCache;
import org.smartboot.socket.protocol.http.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

/**
 * Simulates an apache "combined" style logger, which logs User-Agent, Referer,
 * etc
 *
 * @author Jerome Guibert
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: SimpleAccessLogger.java,v 1.5 2006/03/24 17:24:19 rickknowles
 * Exp $
 */
public final class SimpleAccessLogger implements AccessLogger {
    private final DateCache dateCache;
    private final String pattern;
    protected Logger logger = LogManager.getLogger(getClass());
    private PrintWriter outWriter;

    /**
     * Build a new instance of SimpleAccessLogger.
     *
     * @param host
     * @param webapp
     * @param patternType
     * @param filePattern
     */
    SimpleAccessLogger(final String host, final String webapp, final PatternType patternType, final String filePattern) {
        super();
        dateCache = new DateCache(new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z"));
        pattern = patternType.getPattern();
        final String fileName = StringUtils.replace(filePattern, new String[][]{{"###host###", host}, {"###webapp###", webapp}});

        final File file = new File(fileName);
        file.getParentFile().mkdirs();
        try {
            outWriter = new PrintWriter(new FileOutputStream(file, Boolean.TRUE), Boolean.TRUE);
        } catch (final FileNotFoundException e) {
            logger.error("Unable to open " + fileName, e);
        }

        logger.info(String.format("Initialized access log at %s (format: %s)", fileName, patternType));
    }

    private static String nvl(final String input) {
        return input == null ? "-" : input;
    }

    @Override
    public void log(final String originalURL, final WinstoneRequest request, final WinstoneResponse response) {
        if (outWriter == null) {
            return;
        }
        final String uriLine = request.getMethod() + " " + originalURL + " " + request.getProtocol();
        final int status = response.getErrorStatusCode() == null ? response.getStatus() : response.getErrorStatusCode().intValue();
//		final long size = response.getWinstoneOutputStream().getBytesCommitted();
        final long size = 0;
        final String date = dateCache.now();
        final String logLine = StringUtils.replace(pattern,
                new String[][]{{"###ip###", request.getRemoteHost()}, {"###user###", SimpleAccessLogger.nvl(request.getRemoteUser())}, {"###time###", "[" + date + "]"}, {"###uriLine###", uriLine}, {"###status###", "" + status},
                        {"###size###", "" + size}, {"###referer###", SimpleAccessLogger.nvl(request.getHeader("Referer"))}, {"###userAgent###", SimpleAccessLogger.nvl(request.getHeader("User-Agent"))}});
        outWriter.println(logLine);
    }

    public void destroy() {
        logger.info("Closed access log");
        if (outWriter != null) {
            outWriter.flush();
            outWriter.close();
            outWriter = null;
        }
    }

}
