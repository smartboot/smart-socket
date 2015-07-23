package net.vinote.smart.socket.demo.http.server;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import net.vinote.smart.socket.lang.SmartEnumeration;

public class SmartHttpSession implements HttpSession {
	private long createTime;
	private long lastAccessedTime;
	private String id;
	private int interval;
	private Map<String, Object> attributeMap = new HashMap<String, Object>();

	
	public long getCreationTime() {
		return createTime;
	}

	
	public String getId() {
		return id;
	}

	
	public long getLastAccessedTime() {
		return lastAccessedTime;
	}

	
	public ServletContext getServletContext() {
		return null;
	}

	
	public void setMaxInactiveInterval(int interval) {
		this.interval = interval;
	}

	
	public int getMaxInactiveInterval() {
		return interval;
	}

	
	public HttpSessionContext getSessionContext() {

		return null;
	}

	
	public Object getAttribute(String name) {
		return attributeMap.get(name);
	}

	
	public Object getValue(String name) {
		return null;
	}

	
	public Enumeration<String> getAttributeNames() {
		return new SmartEnumeration<String>(attributeMap.keySet().iterator());
	}

	
	public String[] getValueNames() {
		return null;
	}

	
	public void setAttribute(String name, Object value) {
		attributeMap.put(name, value);
	}

	
	public void putValue(String name, Object value) {

	}

	
	public void removeAttribute(String name) {

	}

	
	public void removeValue(String name) {

	}

	
	public void invalidate() {

	}

	
	public boolean isNew() {
		return false;
	}

}
