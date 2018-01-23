package org.smartboot.socket.protocol.http.accesslog;

/**
 * PatternType enumerate kind of pattern supported : COMMON, COMBINED, RESIN.
 * 
 * @author Jerome Guibert
 */
public enum PatternType {

	COMMON("###ip### - ###user### ###time### \"###uriLine###\" ###status### ###size###"), COMBINED("###ip### - ###user### ###time### \"###uriLine###\" ###status### ###size### \"###referer###\" \"###userAgent###\""), RESIN(
			"###ip### - ###user### ###time### \"###uriLine###\" ###status### ###size### \"###userAgent###\"");
	/** associated pattern */
	private final String pattern;

	/**
	 * Build a new PatternType instance.
	 * 
	 * @param pattern
	 */
	private PatternType(final String pattern) {
		this.pattern = pattern;
	}

	public final String getPattern() {
		return pattern;
	}
	
	public static PatternType fromName(String value) {
	    for (PatternType type: PatternType.values()) {
	        if (type.name().equalsIgnoreCase(value)) {
	            return type;
	        }
	    }
	    return COMBINED;
	}
}
