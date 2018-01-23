/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * A hacked print writer that allows us to trigger an automatic flush on println
 * operations that go over the content length or buffer size.
 * <p>
 * This is only necessary because the spec authors seem intent of having the
 * print writer's flushing behaviour be half auto-flush and half not. Damned if
 * I know why - seems unnecessary and confusing to me.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneResponseWriter.java,v 1.3 2006/02/28 07:32:47
 * rickknowles Exp $
 */
public class WinstoneResponseWriter extends PrintWriter {

    protected static Logger logger = LogManager.getLogger(WinstoneResponseWriter.class);
    private final WinstoneOutputStream outputStream;
    private final WinstoneResponse response;
    private int bytesBuffered;

    public WinstoneResponseWriter(final WinstoneOutputStream out, final WinstoneResponse response) throws UnsupportedEncodingException {
        super(new OutputStreamWriter(out, response.getCharacterEncoding()), Boolean.FALSE);
        outputStream = out;
        this.response = response;
        bytesBuffered = 0;
    }

    @Override
    public void write(final int c) {
        super.write(c);
        appendByteCount("" + ((char) c));
    }

    @Override
    public void write(final char[] buf, final int off, final int len) {
        super.write(buf, off, len);
        if (buf != null) {
            appendByteCount(new String(buf, off, len));
        }
    }

    @Override
    public void write(final String s, final int off, final int len) {
        super.write(s, off, len);
        if (s != null) {
            appendByteCount(s.substring(off, len));
        }
    }

    protected void appendByteCount(final String input) {
        try {
            bytesBuffered += input.getBytes(response.getCharacterEncoding()).length;
        } catch (final IOException err) {/* impossible */

        }

    }

    @Override
    public void println() {
        super.println();
        simulateAutoFlush();
    }

    @Override
    public void flush() {
        super.flush();
        bytesBuffered = 0;
    }

    protected void simulateAutoFlush() {
        final String contentLengthHeader = response.getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER);
        if ((contentLengthHeader != null) && ((outputStream.getOutputStreamLength() + bytesBuffered) >= Integer.parseInt(contentLengthHeader))) {
            WinstoneResponseWriter.logger.debug("Checking for auto-flush of print writer: contentLengthHeader={}, responseBytes={}", contentLengthHeader, (outputStream.getOutputStreamLength() + bytesBuffered) + "");
            flush();
        }
    }
}
