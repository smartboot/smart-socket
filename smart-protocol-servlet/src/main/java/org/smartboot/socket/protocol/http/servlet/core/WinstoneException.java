/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Master exception within the servlet container. This is thrown whenever a
 * non-recoverable error occurs that we want to throw to the top of the
 * application.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneException.java,v 1.1 2004/03/08 15:27:21 rickknowles
 *          Exp $
 */
public class WinstoneException extends RuntimeException {

	private static final long serialVersionUID = 3080421704084165118L;
	private final Throwable nestedError;

	/**
	 * Create an exception with a useful message for the system administrator.
	 * 
	 * @param message
	 *            Error message for to be used for administrative
	 *            troubleshooting
	 */
	public WinstoneException(final String message) {
		this(message, null);
	}

	/**
	 * Create an exception with a useful message for the system administrator
	 * and a nested throwable object.
	 * 
	 * @param message
	 *            Error message for administrative troubleshooting
	 * @param pError
	 *            The actual exception that occurred
	 */
	public WinstoneException(final String message, final Throwable pError) {
		super(message);
		nestedError = pError;
	}

	/**
	 * Get the nested error or exception
	 * 
	 * @return The nested error or exception
	 */
	public Throwable getNestedError() {
		return nestedError;
	}

	@Override
	public void printStackTrace(final PrintWriter p) {
		if (nestedError != null) {
			nestedError.printStackTrace(p);
		}
		p.write("\n");
		super.printStackTrace(p);
	}

	@Override
	public void printStackTrace(final PrintStream p) {
		if (nestedError != null) {
			nestedError.printStackTrace(p);
		}
		p.println("\n");
		super.printStackTrace(p);
	}

	@Override
	public void printStackTrace() {
		if (nestedError != null) {
			nestedError.printStackTrace();
		}
		super.printStackTrace();
	}
}
