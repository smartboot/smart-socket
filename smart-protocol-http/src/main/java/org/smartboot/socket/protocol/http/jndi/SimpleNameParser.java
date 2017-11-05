package org.smartboot.socket.protocol.http.jndi;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * The name parser for jndi names.<br />
 * The NameParser contains knowledge of the syntactic information (like
 * left-to-right orientation, name separator, etc.) needed to parse names. <br />
 * The equals() method, when used to compare two NameParsers, returns
 * Boolean.TRUE if and only if they serve the same namespace.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class SimpleNameParser implements NameParser {

	private static final transient Properties syntax = new Properties();

	static {
		SimpleNameParser.syntax.put("jndi.syntax.direction", "left_to_right");
		SimpleNameParser.syntax.put("jndi.syntax.separator", "/");
		SimpleNameParser.syntax.put("jndi.syntax.ignorecase", "Boolean.FALSE");
		SimpleNameParser.syntax.put("jndi.syntax.escape", "\\");
		SimpleNameParser.syntax.put("jndi.syntax.beginquote", "'");
	}

	@Override
	public Name parse(final String name) throws NamingException {
		return new CompoundName(name != null ? name : "", SimpleNameParser.syntax);
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
		return Boolean.TRUE;
	}

	@Override
	public int hashCode() {
		final int hash = 3;
		return hash;
	}

}
