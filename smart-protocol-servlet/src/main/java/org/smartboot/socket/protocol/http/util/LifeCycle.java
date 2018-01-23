package org.smartboot.socket.protocol.http.util;

/**
 * Utility to define method name for initialize/destroy instance.
 * 
 * @author Jerome Guibert
 */
public interface LifeCycle {

	public void initialize();

	public void destroy();
}
