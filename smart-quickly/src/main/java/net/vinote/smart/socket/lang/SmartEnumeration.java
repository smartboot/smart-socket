package net.vinote.smart.socket.lang;

import java.util.Enumeration;
import java.util.Iterator;

public class SmartEnumeration<K> implements Enumeration<K> {

	private Iterator<K> iteraotr;

	public SmartEnumeration(Iterator<K> iterator) {
		this.iteraotr = iterator;
	}

	
	public boolean hasMoreElements() {
		return iteraotr.hasNext();
	}

	
	public K nextElement() {
		return iteraotr.next();
	}

}
