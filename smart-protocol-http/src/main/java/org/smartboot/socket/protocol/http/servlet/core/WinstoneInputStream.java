/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The request stream management class.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneInputStream.java,v 1.4 2006/02/28 07:32:47 rickknowles
 * Exp $
 */
public class WinstoneInputStream extends javax.servlet.ServletInputStream {

    protected static Logger logger = LogManager.getLogger(WinstoneInputStream.class);
    final int BUFFER_SIZE = 4096;
    private final InputStream inData;
    private Integer contentLength;
    private int readSoFar;

    /**
     * Constructor
     */
    public WinstoneInputStream(final InputStream inData) {
        super();
        this.inData = inData;
    }

    public WinstoneInputStream(final byte inData[]) {
        this(new ByteArrayInputStream(inData));
    }

    public InputStream getRawInputStream() {
        return inData;
    }

    public void setContentLength(final int length) {
        contentLength = new Integer(length);
        readSoFar = 0;
    }

    @Override
    public int read() throws IOException {
        if (contentLength == null) {
            final int data = inData.read();
            return data;
        } else if (contentLength.intValue() > readSoFar) {
            readSoFar++;
            final int data = inData.read();
            return data;
        } else {
            return -1;
        }
    }

    @Override
    public int read(final byte[] b, final int off, int len) throws IOException {
        if (contentLength == null) {
            return inData.read(b, off, len);
        } else {
            len = Math.min(len, contentLength.intValue() - readSoFar);
            if (len <= 0) {
                return -1;
            }
            final int r = inData.read(b, off, len);
            if (r < 0) {
                return r; // EOF
            }
            readSoFar += r;
            return r;
        }

    }

    /**
     * Reads like {@link DataInputStream#readFully(byte[], int, int)}, except
     * EOF before fully reading it won't result in an exception.
     *
     * @return number of bytes read.
     */
    public int readAsMuchAsPossible(final byte[] buf, final int offset, final int len) throws IOException {
        int total = 0;
        while (total < len) {
            final int count = read(buf, offset + total, len - total);
            if (count < 0) {
                break;
            }
            total += count;
        }
        return total;
    }

    public void finishRequest() {
        // this.inData = null;
        // byte content[] = this.dump.toByteArray();
        // com.rickknowles.winstone.ajp13.Ajp13Listener.packetDump(content,
        // content.length);
    }

    @Override
    public int available() throws IOException {
        return inData.available();
    }

    /**
     * Wrapper for the servletInputStream's readline method
     */
    public byte[] readLine() throws IOException {
        final byte buffer[] = new byte[BUFFER_SIZE];
        final int charsRead = super.readLine(buffer, 0, BUFFER_SIZE);
        if (charsRead == -1) {
            WinstoneInputStream.logger.debug("End of stream");
            return new byte[0];
        }
        final byte outBuf[] = new byte[charsRead];
        System.arraycopy(buffer, 0, outBuf, 0, charsRead);
        return outBuf;
    }
}
