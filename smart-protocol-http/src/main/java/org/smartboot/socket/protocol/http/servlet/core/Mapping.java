/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Encapsulates the parsing of URL patterns, as well as the mapping of a url
 * pattern to a servlet instance
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Mapping.java,v 1.9 2007/04/23 02:55:35 rickknowles Exp $
 */
public class Mapping implements java.util.Comparator<Mapping> {

    public static final int EXACT_PATTERN = 1;
    public static final int FOLDER_PATTERN = 2;
    public static final int EXTENSION_PATTERN = 3;
    public static final int DEFAULT_SERVLET = 4;
    public static final String STAR = "*";
    public static final String SLASH = "/";
    protected static Logger logger = LogManager.getLogger(Mapping.class);
    // name
    private final String mappedTo;
    private String urlPattern;
    private String linkName; // used to map filters to a specific servlet by
    private int patternType;
    private boolean isPatternFirst; // ie is this a blah* pattern, not *blah

    // (extensions only)
    protected Mapping(final String mappedTo) {
        this.mappedTo = mappedTo;
    }

    /**
     * Factory constructor method - this parses the url pattern into pieces we
     * can use to match against incoming URLs.
     */
    public static Mapping createFromURL(final String mappedTo, String pattern) {
        if ((pattern == null) || (mappedTo == null)) {
            throw new WinstoneException("WebAppConfig: Invalid pattern mount for " + mappedTo + " pattern " + pattern + " - ignoring");
        }

        // Compatibility hacks - add a leading slash if one is not found and not
        // an extension mapping
        if (!pattern.equals("") && !pattern.startsWith(Mapping.STAR) && !pattern.startsWith(Mapping.SLASH)) {
            pattern = Mapping.SLASH + pattern;
        } else if (pattern.equals(Mapping.STAR)) {
            Mapping.logger.warn("WARNING: Invalid \"*\" only mount. Interpreting as a \"/*\" mount");
            pattern = Mapping.SLASH + Mapping.STAR;
        }

        final Mapping me = new Mapping(mappedTo);

        final int firstStarPos = pattern.indexOf(Mapping.STAR);
        final int lastStarPos = pattern.lastIndexOf(Mapping.STAR);
        final int patternLength = pattern.length();

        // check for default servlet, ie mapping = exactly /
        if (pattern.equals(Mapping.SLASH)) {
            me.urlPattern = "";
            me.patternType = Mapping.DEFAULT_SERVLET;
        } else if (firstStarPos == -1) {
            me.urlPattern = pattern;
            me.patternType = Mapping.EXACT_PATTERN;
        } // > 1 star = error
        else if (firstStarPos != lastStarPos) {
            throw new WinstoneException("WebAppConfig: Invalid pattern mount for " + mappedTo + " pattern " + pattern + " - ignoring");
        } // check for folder style mapping (ends in /*)
        else if (pattern.indexOf(Mapping.SLASH + Mapping.STAR) == (patternLength - (Mapping.SLASH + Mapping.STAR).length())) {
            me.urlPattern = pattern.substring(0, pattern.length() - (Mapping.SLASH + Mapping.STAR).length());
            me.patternType = Mapping.FOLDER_PATTERN;
        } // check for non-extension match
        else if (pattern.indexOf(Mapping.SLASH) != -1) {
            throw new WinstoneException("WebAppConfig: Invalid pattern mount for " + mappedTo + " pattern " + pattern + " - ignoring");
        } // check for extension match at the beginning (eg *blah)
        else if (firstStarPos == 0) {
            me.urlPattern = pattern.substring(Mapping.STAR.length());
            me.patternType = Mapping.EXTENSION_PATTERN;
            me.isPatternFirst = Boolean.FALSE;
        } // check for extension match at the end (eg blah*)
        else if (firstStarPos == (patternLength - Mapping.STAR.length())) {
            me.urlPattern = pattern.substring(0, patternLength - Mapping.STAR.length());
            me.patternType = Mapping.EXTENSION_PATTERN;
            me.isPatternFirst = Boolean.TRUE;
        } else {
            throw new WinstoneException("WebAppConfig: Invalid pattern mount for " + mappedTo + " pattern " + pattern + " - ignoring");
        }
        Mapping.logger.debug("Mapped: {} to {}", mappedTo, pattern);
        return me;
    }

    /**
     * Factory constructor method - this turns a servlet name into a mapping
     * element
     */
    public static Mapping createFromLink(final String mappedTo, final String linkName) {
        if ((linkName == null) || (mappedTo == null)) {
            throw new WinstoneException("WebAppConfig: Invalid link mount for " + mappedTo + " link " + linkName + " - ignoring");
        }

        final Mapping me = new Mapping(mappedTo);
        me.linkName = linkName;
        return me;
    }

    public int getPatternType() {
        return patternType;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public String getMappedTo() {
        return mappedTo;
    }

    public String getLinkName() {
        return linkName;
    }

    /**
     * Try to match this pattern against the incoming url
     *
     * @param inputPattern The URL we want to check for a match
     * @param servletPath  An empty StringBuilder for the servletPath of a successful
     *                     match
     * @param pathInfo     An empty StringBuilder for the pathInfo of a successful match
     * @return Boolean.TRUE if the match is successful
     */
    public boolean match(final String inputPattern, final StringBuilder servletPath, final StringBuilder pathInfo) {
        switch (patternType) {
            case FOLDER_PATTERN:
                if (inputPattern.startsWith(urlPattern + '/') || inputPattern.equals(urlPattern)) {
                    if (servletPath != null) {
                        servletPath.append(WinstoneRequest.decodePathURLToken(urlPattern));
                    }
                    if (pathInfo != null) {
                        pathInfo.append(WinstoneRequest.decodeURLToken(inputPattern.substring(urlPattern.length())));
                    }
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }

            case EXTENSION_PATTERN:
                // Strip down to the last item in the path
                final int slashPos = inputPattern.lastIndexOf(Mapping.SLASH);
                if ((slashPos == -1) || (slashPos == (inputPattern.length() - 1))) {
                    return Boolean.FALSE;
                }
                final String fileName = inputPattern.substring(slashPos + 1);
                if ((isPatternFirst && fileName.startsWith(urlPattern)) || (!isPatternFirst && fileName.endsWith(urlPattern))) {
                    if (servletPath != null) {
                        servletPath.append(WinstoneRequest.decodePathURLToken(inputPattern));
                    }
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }

            case EXACT_PATTERN:
                if (inputPattern.equals(urlPattern)) {
                    if (servletPath != null) {
                        servletPath.append(WinstoneRequest.decodePathURLToken(inputPattern));
                    }
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }

            case DEFAULT_SERVLET:
                if (servletPath != null) {
                    servletPath.append(WinstoneRequest.decodePathURLToken(inputPattern));
                }
                return Boolean.TRUE;

            default:
                return Boolean.FALSE;
        }
    }

    @Override
    public String toString() {
        return linkName != null ? "Link:" + linkName : "URLPattern:type=" + patternType + ",pattern=" + urlPattern;
    }

    @Override
    public int compare(final Mapping one, final Mapping two) {
        final Integer intOne = new Integer(one.getPatternType());
        final Integer intTwo = new Integer(two.getPatternType());
        final int order = -1 * intOne.compareTo(intTwo);
        if (order != 0) {
            return order;
        }
        if (one.getLinkName() != null) {
            // servlet name mapping - just alphabetical sort
            return one.getLinkName().compareTo(two.getLinkName());
        } else {
            return -1 * one.getUrlPattern().compareTo(two.getUrlPattern());
        }
    }
}
