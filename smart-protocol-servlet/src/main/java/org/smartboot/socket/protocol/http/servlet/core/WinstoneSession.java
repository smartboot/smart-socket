/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Http session implementation for Winstone.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneSession.java,v 1.10 2006/08/27 07:19:47 rickknowles Exp
 * $
 */
public class WinstoneSession implements HttpSession, Serializable {

    /**
     * Generated serial Version UID
     */
    private static final long serialVersionUID = 6106594480472753553L;
    public static String SESSION_COOKIE_NAME = "JSESSIONID";
    protected static Logger logger = LogManager.getLogger(WinstoneSession.class);
    private String sessionId;
    private WebAppConfiguration webAppConfig;
    private Map<String, Object> sessionData;
    private Set<WinstoneRequest> requestsUsingMe;
    private long createTime;
    private long lastAccessedTime;
    private int maxInactivePeriod;
    private boolean isNew;
    private boolean isInvalidated;
    private HttpSessionAttributeListener sessionAttributeListeners[];
    private HttpSessionListener sessionListeners[];
    private HttpSessionActivationListener sessionActivationListeners[];
    private Object sessionMonitor = Boolean.TRUE;

    /**
     * Constructor
     */
    public WinstoneSession(final String sessionId) {
        super();
        this.sessionId = sessionId;
        this.sessionData = new Hashtable<String, Object>();
        this.requestsUsingMe = Collections.synchronizedSet(new HashSet<WinstoneRequest>());
        this.createTime = System.currentTimeMillis();
        this.isNew = Boolean.TRUE;
        this.isInvalidated = Boolean.FALSE;
    }

    public static File getSessionTempDir(final WebAppConfiguration webAppConfig) {
        final File tmpDir = (File) webAppConfig.getAttribute("javax.servlet.context.tempdir");
        final File sessionsDir = new File(tmpDir, "WEB-INF" + File.separator + "winstoneSessions");
        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs();
        }
        return sessionsDir;
    }

    public static void loadSessions(final WebAppConfiguration webAppConfig) {
        int expiredCount = 0;
        // Iterate through the files in the dir, instantiate and then add to the
        // sessions set
        final File tempDir = WinstoneSession.getSessionTempDir(webAppConfig);
        final File possibleSessionFiles[] = tempDir.listFiles();
        for (int n = 0; n < possibleSessionFiles.length; n++) {
            if (possibleSessionFiles[n].getName().endsWith(".ser")) {
                InputStream in = null;
                ObjectInputStream objIn = null;
                try {
                    in = new FileInputStream(possibleSessionFiles[n]);
                    objIn = new ObjectInputStream(in);
                    final WinstoneSession session = (WinstoneSession) objIn.readObject();
                    session.setWebAppConfiguration(webAppConfig);
                    webAppConfig.setSessionListeners(session);
                    if (session.isExpired()) {
                        session.invalidate();
                        expiredCount++;
                    } else {
                        webAppConfig.addSession(session.getId(), session);
                        WinstoneSession.logger.debug("Successfully restored session id {} from temp space", session.getId());
                    }
                } catch (final Throwable err) {
                    WinstoneSession.logger.error("Error loading session from temp space - skipping. Error:", err);
                } finally {
                    if (objIn != null) {
                        try {
                            objIn.close();
                        } catch (final IOException err) {
                        }
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (final IOException err) {
                        }
                    }
                    possibleSessionFiles[n].delete();
                }
            }
        }
        if (expiredCount > 0) {
            WinstoneSession.logger.debug(expiredCount + " Session(s) has been invalidated");
        }
    }

    public void setWebAppConfiguration(final WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
    }

    public void sendCreatedNotifies() {
        // Notify session listeners of new session
        for (int n = 0; n < sessionListeners.length; n++) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
            sessionListeners[n].sessionCreated(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void setSessionActivationListeners(final HttpSessionActivationListener listeners[]) {
        sessionActivationListeners = listeners;
    }

    public void setSessionAttributeListeners(final HttpSessionAttributeListener listeners[]) {
        sessionAttributeListeners = listeners;
    }

    public void setSessionListeners(final HttpSessionListener listeners[]) {
        sessionListeners = listeners;
    }

    public void setLastAccessedDate(final long time) {
        lastAccessedTime = time;
    }

    public void setIsNew(final boolean isNew) {
        this.isNew = isNew;
    }

    public void addUsed(final WinstoneRequest request) {
        requestsUsingMe.add(request);
    }

    public void removeUsed(final WinstoneRequest request) {
        requestsUsingMe.remove(request);
    }

    public boolean isUnusedByRequests() {
        return requestsUsingMe.isEmpty();
    }

    public boolean isExpired() {
        // check if it's expired yet
        final long nowDate = System.currentTimeMillis();
        final long maxInactive = getMaxInactiveInterval() * 1000;
        return ((maxInactive > 0) && ((nowDate - lastAccessedTime) > maxInactive));
    }

    // Implementation methods
    @Override
    public Object getAttribute(final String name) {
        if (isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        Object att = null;
        synchronized (sessionMonitor) {
            att = sessionData.get(name);
        }
        return att;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        if (isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        Enumeration<String> names = null;
        synchronized (sessionMonitor) {
            names = Collections.enumeration(sessionData.keySet());
        }
        return names;
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        if (isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }

        // valueBound must be before binding
        if (value instanceof HttpSessionBindingListener) {
            final HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
            hsbl.valueBound(new HttpSessionBindingEvent(this, name, value));
            Thread.currentThread().setContextClassLoader(cl);
        }

        Object oldValue = null;
        synchronized (sessionMonitor) {
            oldValue = sessionData.get(name);
            if (value == null) {
                sessionData.remove(name);
            } else {
                sessionData.put(name, value);
            }
        }

        // valueUnbound must be after unbinding
        if (oldValue instanceof HttpSessionBindingListener) {
            final HttpSessionBindingListener hsbl = (HttpSessionBindingListener) oldValue;
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
            hsbl.valueUnbound(new HttpSessionBindingEvent(this, name, oldValue));
            Thread.currentThread().setContextClassLoader(cl);
        }

        // Notify other listeners
        if (oldValue != null) {
            for (int n = 0; n < sessionAttributeListeners.length; n++) {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
                sessionAttributeListeners[n].attributeReplaced(new HttpSessionBindingEvent(this, name, oldValue));
                Thread.currentThread().setContextClassLoader(cl);
            }
        } else {
            for (int n = 0; n < sessionAttributeListeners.length; n++) {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
                sessionAttributeListeners[n].attributeAdded(new HttpSessionBindingEvent(this, name, value));
                Thread.currentThread().setContextClassLoader(cl);

            }
        }
    }

    @Override
    public void removeAttribute(final String name) {
        if (isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        Object value = null;
        synchronized (sessionMonitor) {
            value = sessionData.get(name);
            sessionData.remove(name);
        }

        // Notify listeners
        if (value instanceof HttpSessionBindingListener) {
            final HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
            hsbl.valueUnbound(new HttpSessionBindingEvent(this, name));
            Thread.currentThread().setContextClassLoader(cl);
        }
        if (value != null) {
            for (int n = 0; n < sessionAttributeListeners.length; n++) {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
                sessionAttributeListeners[n].attributeRemoved(new HttpSessionBindingEvent(this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    @Override
    public long getCreationTime() {
        if (isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        return createTime;
    }

    @Override
    public long getLastAccessedTime() {
        if (isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        return lastAccessedTime;
    }

    @Override
    public String getId() {
        return sessionId;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactivePeriod;
    }

    @Override
    public void setMaxInactiveInterval(final int interval) {
        maxInactivePeriod = interval;
    }

    @Override
    public boolean isNew() {
        if (isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        return isNew;
    }

    @Override
    public ServletContext getServletContext() {
        return webAppConfig;
    }

    @Override
    public void invalidate() {
        if (isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        // Notify session listeners of invalidated session -- backwards
        for (int n = sessionListeners.length - 1; n >= 0; n--) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
            sessionListeners[n].sessionDestroyed(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }

        final List<String> keys = new ArrayList<String>(sessionData.keySet());
        for (final Iterator<String> i = keys.iterator(); i.hasNext(); ) {
            removeAttribute(i.next());
        }
        synchronized (sessionMonitor) {
            sessionData.clear();
        }
        isInvalidated = Boolean.TRUE;
        webAppConfig.removeSessionById(sessionId);
    }

    /**
     * Called after the session has been serialized to another server.
     */
    public void passivate() {
        // Notify session listeners of invalidated session
        for (int n = 0; n < sessionActivationListeners.length; n++) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
            sessionActivationListeners[n].sessionWillPassivate(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }

        // Question: Is passivation equivalent to invalidation ? Should all
        // entries be removed ?
        // List keys = new ArrayList(this.sessionData.keySet());
        // for (Iterator i = keys.iterator(); i.hasNext(); )
        // removeAttribute((String) i.next());
        synchronized (sessionMonitor) {
            sessionData.clear();
        }
        webAppConfig.removeSessionById(sessionId);
    }

    /**
     * Called after the session has been deserialized from another server.
     */
    public void activate(final WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
        webAppConfig.setSessionListeners(this);

        // Notify session listeners of invalidated session
        for (int n = 0; n < sessionActivationListeners.length; n++) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            sessionActivationListeners[n].sessionDidActivate(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Save this session to the temp dir defined for this webapp
     */
    public void saveToTemp() {
        final File toDir = WinstoneSession.getSessionTempDir(webAppConfig);
        synchronized (sessionMonitor) {
            OutputStream out = null;
            ObjectOutputStream objOut = null;
            try {
                final File toFile = new File(toDir, sessionId + ".ser");
                out = new FileOutputStream(toFile, Boolean.FALSE);
                objOut = new ObjectOutputStream(out);
                objOut.writeObject(this);
            } catch (final IOException err) {
                WinstoneSession.logger.error("Error saving the session to temp space. Error:", err);
            } finally {
                if (objOut != null) {
                    try {
                        objOut.close();
                    } catch (final IOException err) {
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (final IOException err) {
                    }
                }
            }
        }
    }

    /**
     * Serialization implementation. This makes sure to only serialize the parts
     * we want to send to another server.
     *
     * @param out The stream to write the contents to
     * @throws IOException
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.writeUTF(sessionId);
        out.writeLong(createTime);
        out.writeLong(lastAccessedTime);
        out.writeInt(maxInactivePeriod);
        out.writeBoolean(isNew);

        // Write the map, but first remove non-serializables
        final Map<String, Object> copy = new HashMap<String, Object>(sessionData);
        final Set<String> keys = new HashSet<String>(copy.keySet());
        for (final Iterator<String> i = keys.iterator(); i.hasNext(); ) {
            final String key = i.next();
            if (!(copy.get(key) instanceof Serializable)) {
                WinstoneSession.logger.warn("Web application is marked distributable, but session object {} (class {}) does not extend java.io.Serializable - this variable is being ignored by session transfer", key, copy.get(key).getClass()
                        .getName());
                copy.remove(key);
            }
        }
        out.writeInt(copy.size());
        for (final Iterator<String> i = copy.keySet().iterator(); i.hasNext(); ) {
            final String key = i.next();
            out.writeUTF(key);
            out.writeObject(copy.get(key));
        }
    }

    /**
     * Deserialization implementation
     *
     * @param in The source of stream data
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        sessionId = in.readUTF();
        createTime = in.readLong();
        lastAccessedTime = in.readLong();
        maxInactivePeriod = in.readInt();
        isNew = in.readBoolean();

        // Read the map
        sessionData = new Hashtable<String, Object>();
        requestsUsingMe = Collections.synchronizedSet(new HashSet<WinstoneRequest>());
        final int entryCount = in.readInt();
        for (int n = 0; n < entryCount; n++) {
            final String key = in.readUTF();
            final Object variable = in.readObject();
            sessionData.put(key, variable);
        }
        sessionMonitor = Boolean.TRUE;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public Object getValue(final String name) {
        return getAttribute(name);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void putValue(final String name, final Object value) {
        setAttribute(name, value);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void removeValue(final String name) {
        removeAttribute(name);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public String[] getValueNames() {
        return sessionData.keySet().toArray(new String[0]);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return null;
    } // deprecated

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
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
        final WinstoneSession other = (WinstoneSession) obj;
        if (sessionId == null) {
            if (other.sessionId != null) {
                return Boolean.FALSE;
            }
        } else if (!sessionId.equals(other.sessionId)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    public String toString() {
        return "WinstoneSession [sessionId=" + sessionId + "]";
    }
}
