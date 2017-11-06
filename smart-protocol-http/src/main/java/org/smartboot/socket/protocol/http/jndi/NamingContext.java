/* 
 * Copyright 1997, 1998, 1999 Sun Microsystems, Inc. All Rights
 * Reserved.
 * 
 * Sun grants you ("Licensee") a non-exclusive, royalty free,
 * license to use, modify and redistribute this software in source and
 * binary code form, provided that i) this copyright notice and license
 * appear on all copies of the software; and ii) Licensee does not 
 * utilize the software in a manner which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE 
 * HEREBY EXCLUDED.  SUN AND ITS LICENSORS SHALL NOT BE LIABLE 
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, 
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN 
 * NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST 
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER 
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT 
 * OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS 
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and warrants
 * that it will not use or redistribute the Software for such purposes.  
 */

package org.smartboot.socket.protocol.http.jndi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;

/**
 * <code>NamingContext</code> class is based on HierarchicalNamingContext (from
 * jndi tutorial) and older <code>WinstoneContext</code> class.<br />
 * List of change:<br />
 * <ul>
 * <li>Implement Names and Bindings enumeration</li>
 * <li>Rename and type all member</li>
 * <li>Remove use of clone()</li>
 * <li>Add final on all parameter and attribut (as possible)</li>
 * <li>Fix potential problems</li>
 * <li>Import Rick Knowles work on previous WinstonContext</li>
 * </ul>
 *
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class NamingContext implements Context {

    protected final static transient NameParser nameParser = new SimpleNameParser();
    private static final transient String PREFIX = "java:";
    private static final transient String FIRST_CHILD = "comp";
    private static final transient String BODGED_PREFIX = "java:comp";
    /**
     * binding map
     */
    protected final Hashtable<String, Object> bindings;
    /**
     * context parent if exist
     */
    protected final NamingContext parent;
    /**
     * atomic name os this context
     */
    protected final String myAtomicName;
    protected Logger logger = LogManager.getLogger(getClass());
    /**
     * environnement Map (using lazy instanciation)
     */
    protected Hashtable<String, Object> environnement;

    /**
     * Build a new instance of HierarchicalNamingContext.
     *
     * @param environnement
     */
    public NamingContext(final Hashtable<String, Object> environnement) {
        this(null, environnement);
    }

    /**
     * Build a new instance of HierarchicalNamingContext.
     *
     * @param name
     * @param environnement
     */
    public NamingContext(final String name, final Hashtable<String, Object> environnement) {
        this(null, name, environnement, null);
    }

    /**
     * Used to build new sub context or clone this instance.
     *
     * @param parent
     * @param name
     * @param environnement
     * @param bindings
     */
    protected NamingContext(final NamingContext parent, final String name, final Hashtable<String, Object> environnement, final Hashtable<String, Object> bindings) {
        super();
        this.parent = parent;
        myAtomicName = name;
        if (environnement != null) {
            this.environnement = new Hashtable<String, Object>();
            this.environnement.putAll(environnement);
        }
        this.bindings = new Hashtable<String, Object>();
        if (bindings != null) {
            this.bindings.putAll(bindings);
        }

    }

    /**
     * Utility method for processing composite/compound name.<br />
     * Handles the processing of relative and absolute names. If a relative name
     * is detected, it is processed by the name parser. If an absolute name is
     * detected, it determines first if the absolute name refers to this
     * context. If not, it then determines whether the request can be passed
     * back to the parent or not, and returns null if it can, and throws an
     * exception otherwise.
     *
     * @param name
     * @return
     */
    private Name validateName(final Name name) throws NamingException {
        // Check for absolute urls and redirect or correct
        if (name.isEmpty()) {
            return name;
        } else if (name.get(0).equals(NamingContext.BODGED_PREFIX)) {
            final Name newName = name.getSuffix(1).add(0, NamingContext.FIRST_CHILD).add(0, NamingContext.PREFIX);
            return validateName(newName);
        } else if (name.get(0).equals(NamingContext.PREFIX)) {
            final String nameInNamespace = getNameInNamespace();
            final String stringName = name.toString();
            if (stringName.equals(nameInNamespace)) {
                return NamingContext.nameParser.parse("");
            } else if (nameInNamespace.equals("")) {
                return NamingContext.nameParser.parse(stringName);
            } else if (stringName.startsWith(nameInNamespace)) {
                return NamingContext.nameParser.parse(stringName.substring(nameInNamespace.length() + 1));
            } else if (parent != null) {
                return null;
            }
            throw new NameNotFoundException("Name '" + name.toString() + "' Not Found");
        } else if (name instanceof CompositeName) {
            return NamingContext.nameParser.parse(name.toString());
        }
        return name;
    }

    @Override
    public Object lookup(final String name) throws NamingException {
        logger.trace("lookup {}", name);
        return lookup(new CompositeName(name));
    }

    @Override
    public Object lookup(final Name name) throws NamingException {
        final Name searchName = validateName(name);

        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (searchName == null) {
            logger.debug("lookup parent for {}", name);
            return parent.lookup(name);
        }

        if (searchName.isEmpty()) {
            // Asking to look up this context itself. Create and return
            // a new instance with its own independent environment.
            // Clone this instance.
            return new NamingContext(parent, myAtomicName, environnement, bindings);
        }
        // Extract components that belong to this namespace
        final Name nm = searchName; // getMyComponents(name);
        final String atom = nm.get(0);
        final Object inter = bindings.get(atom);
        if (nm.size() == 1) {
            // Atomic name: Find object in internal data structure
            if (inter == null) {
                logger.debug("{} not found", name);
                throw new NameNotFoundException(name + " not found");
            }
            // Call getObjectInstance for using any object factories
            try {
                return NamingManager.getObjectInstance(inter, new CompositeName().add(atom), this, environnement);
            } catch (final Exception e) {
                logger.debug("getObjectInstance failed for {} ", atom);
                final NamingException ne = new NamingException("getObjectInstance failed");
                ne.setRootCause(e);
                throw ne;
            }
        } else {
            // Intermediate name: Consume name in this context and continue
            if (!(inter instanceof Context)) {
                logger.debug("{} does not name a context", atom);
                throw new NotContextException(atom + " does not name a context");
            }

            return ((Context) inter).lookup(nm.getSuffix(1));
        }
    }

    @Override
    public void bind(final String name, final Object obj) throws NamingException {
        bind(new CompositeName(name), obj);
    }

    @Override
    public void bind(final Name name, final Object obj) throws NamingException {
        final Name bindName = validateName(name);
        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (bindName == null) {
            parent.bind(name, obj);
        }
        // If empty name, complain - we should have a child name here
        if (bindName.isEmpty()) {
            throw new InvalidNameException("Cannot rebind an empty name");
        }

        // Extract components that belong to this namespace
        final Name nm = bindName; // getMyComponents(name);
        final String atom = nm.get(0);
        final Object inter = bindings.get(atom);

        if (nm.size() == 1) {
            // Atomic name: Find object in internal data structure
            if (inter != null) {
                throw new NameAlreadyBoundException("Use rebind to override");
            }

            // Call getStateToBind for using any state factories
            final Object result = NamingManager.getStateToBind(obj, new CompositeName().add(atom), this, environnement);

            // Add object to internal data structure
            bindings.put(atom, result);
        } else {
            // Intermediate name: Consume name in this context and continue
            if (!(inter instanceof Context)) {
                throw new NotContextException(atom + " does not name a context");
            }
            ((Context) inter).bind(nm.getSuffix(1), obj);
        }
    }

    @Override
    public void rebind(final String name, final Object obj) throws NamingException {
        rebind(new CompositeName(name), obj);
    }

    @Override
    public void rebind(final Name name, final Object obj) throws NamingException {
        final Name bindName = validateName(name);
        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (bindName == null) {
            parent.bind(name, obj);
        }
        // If empty name, complain - we should have a child name here
        if (bindName.isEmpty()) {
            throw new InvalidNameException("Cannot rebind an empty name");
        }

        // Extract components that belong to this namespace
        final Name nm = bindName; // getMyComponents(name);
        final String atom = nm.get(0);

        if (nm.size() == 1) {
            // Atomic name

            // Call getStateToBind for using any state factories
            final Object result = NamingManager.getStateToBind(obj, new CompositeName().add(atom), this, environnement);

            // Add object to internal data structure
            bindings.put(atom, result);
        } else {
            // Intermediate name: Consume name in this context and continue
            final Object inter = bindings.get(atom);
            if (!(inter instanceof Context)) {
                throw new NotContextException(atom + " does not name a context");
            }
            ((Context) inter).rebind(nm.getSuffix(1), obj);
        }
    }

    @Override
    public void unbind(final String name) throws NamingException {
        unbind(new CompositeName(name));
    }

    @Override
    public void unbind(final Name name) throws NamingException {
        final Name unbindName = validateName(name);

        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (unbindName == null) {
            parent.unbind(name);
        } else if (unbindName.isEmpty()) {
            throw new InvalidNameException("Cannot unbind empty name");
        }

        // Extract components that belong to this namespace
        final Name nm = unbindName;// getMyComponents(name);
        final String atom = nm.get(0);

        // Remove object from internal data structure
        if (nm.size() == 1) {
            // Atomic name: Find object in internal data structure
            bindings.remove(atom);
        } else {
            // Intermediate name: Consume name in this context and continue
            final Object inter = bindings.get(atom);
            if (!(inter instanceof Context)) {
                throw new NotContextException(atom + " does not name a context");
            }
            ((Context) inter).unbind(nm.getSuffix(1));
        }
    }

    @Override
    public void rename(final String oldname, final String newname) throws NamingException {
        rename(new CompositeName(oldname), new CompositeName(newname));
    }

    @Override
    public void rename(final Name oldname, final Name newname) throws NamingException {
        if (oldname.isEmpty() || newname.isEmpty()) {
            throw new InvalidNameException("Cannot rename empty name");
        }

        // Extract components that belong to this namespace
        final Name oldnm = validateName(oldname);// getMyComponents(oldname);
        final Name newnm = validateName(newname); // getMyComponents(newname);
        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (oldnm == null) {
            parent.rename(oldname, newname);
        } else if (oldname.isEmpty()) {
            throw new InvalidNameException("Cannot rename from an empty name");
        }

        // Simplistic implementation: support only rename within same context
        if (oldnm.size() != newnm.size()) {
            throw new OperationNotSupportedException("Do not support rename across different contexts");
        }

        final String oldatom = oldnm.get(0);
        final String newatom = newnm.get(0);

        if (oldnm.size() == 1) {
            // Atomic name: Add object to internal data structure
            // Check if new name exists
            if (bindings.get(newatom) != null) {
                throw new NameAlreadyBoundException(newname.toString() + " is already bound");
            }

            // Check if old name is bound
            final Object oldBinding = bindings.remove(oldatom);
            if (oldBinding == null) {
                throw new NameNotFoundException(oldname.toString() + " not bound");
            }

            bindings.put(newatom, oldBinding);
        } else {
            // Simplistic implementation: support only rename within same
            // context
            if (!oldatom.equals(newatom)) {
                throw new OperationNotSupportedException("Do not support rename across different contexts");
            }

            // Intermediate name: Consume name in this context and continue
            final Object inter = bindings.get(oldatom);
            if (!(inter instanceof Context)) {
                throw new NotContextException(oldatom + " does not name a context");
            }
            ((Context) inter).rename(oldnm.getSuffix(1), newnm.getSuffix(1));
        }
    }

    @Override
    public NamingEnumeration<NameClassPair> list(final String name) throws NamingException {
        return list(new CompositeName(name));
    }

    @Override
    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        final Name searchName = validateName(name);
        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (searchName == null) {
            return parent.list(name);
        } else if (searchName.isEmpty()) {
            // listing this context
            return new Names(bindings);
        }

        // Perhaps 'name' names a context
        final Object target = lookup(name);
        if (target instanceof Context) {
            return ((Context) target).list("");
        }
        throw new NotContextException(name + " cannot be listed");
    }

    @Override
    public NamingEnumeration<Binding> listBindings(final String name) throws NamingException {
        return listBindings(new CompositeName(name));
    }

    @Override
    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        final Name searchName = validateName(name);
        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (searchName == null) {
            return parent.listBindings(name);
        }
        if (searchName.isEmpty()) {
            // listing this context
            return new Bindings(this, environnement, bindings);
        }

        // Perhaps 'name' names a context
        final Object target = lookup(name);
        if (target instanceof Context) {
            return ((Context) target).listBindings("");
        }
        throw new NotContextException(name + " cannot be listed");
    }

    @Override
    public void destroySubcontext(final String name) throws NamingException {
        destroySubcontext(new CompositeName(name));
    }

    @Override
    public void destroySubcontext(final Name name) throws NamingException {
        final Name childName = validateName(name);
        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (childName == null) {
            parent.destroySubcontext(name);
        }
        // checking for nonempty context first
        if (childName.isEmpty()) {
            if (!name.isEmpty() && (name.size() > 1)) {
                parent.destroySubcontext(name.getSuffix(name.size() - 2));
                return;
            }
            throw new InvalidNameException("Cannot destroy context using empty name");
        }
        // Use same implementation as unbind
        unbind(name);
    }

    @Override
    public Context createSubcontext(final String name) throws NamingException {
        return createSubcontext(new CompositeName(name));
    }

    @Override
    public Context createSubcontext(final Name name) throws NamingException {
        final Name childName = validateName(name);
        // If null, it means we don't know how to handle this -> throw to the
        // parent
        if (childName == null) {
            return parent.createSubcontext(name);
            // If empty name, complain - we should have a child name here
        }

        if (childName.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }
        // Extract components that belong to this namespace
        final Name nm = childName;// getMyComponents(name);
        final String atom = nm.get(0);
        final Object inter = bindings.get(atom);
        if (nm.size() == 1) {
            // Atomic name: Find object in internal data structure
            if (inter != null) {
                throw new NameAlreadyBoundException("Use rebind to override");
            }
            // Create child
            final Context child = new NamingContext(this, atom, environnement, null);
            // Add child to internal data structure
            bindings.put(atom, child);
            return child;
        } else {
            // Intermediate name: Consume name in this context and continue
            if (!(inter instanceof Context)) {
                throw new NotContextException(atom + " does not name a context");
            }
            return ((Context) inter).createSubcontext(nm.getSuffix(1));
        }
    }

    @Override
    public Object lookupLink(final String name) throws NamingException {
        return lookupLink(new CompositeName(name));
    }

    @Override
    public Object lookupLink(final Name name) throws NamingException {
        final Object result = lookup(name);
        if (result instanceof LinkRef) {
            final LinkRef ref = (LinkRef) result;
            final String link = ref.getLinkName();
            if (link.startsWith("./")) {
                // relative link; assume currCtx is the immediate context in
                // which the link is bound
                return lookup(link.substring(2));
            } else {
                // absolute link; resolve to the initial context
                return lookupLink(link);
            }
        }
        return result;
    }

    @Override
    public NameParser getNameParser(final String name) throws NamingException {
        return getNameParser(new CompositeName(name));
    }

    @Override
    public NameParser getNameParser(final Name name) throws NamingException {
        // Do lookup to verify name exists
        final Object obj = lookup(name);
        if (obj instanceof Context) {
            ((Context) obj).close();
        }
        return NamingContext.nameParser;
    }

    @Override
    public String composeName(final String name, final String prefix) throws NamingException {
        final Name result = composeName(new CompositeName(name), new CompositeName(prefix));
        return result.toString();
    }

    @Override
    public Name composeName(final Name name, final Name prefix) throws NamingException {
        Name result;

        // Both are compound names, compose using compound name rules
        if (!(name instanceof CompositeName) && !(prefix instanceof CompositeName)) {
            result = (Name) (prefix.clone());
            result.addAll(name);
            return new CompositeName().add(result.toString());
        }

        // Simplistic implementation: do not support federation
        throw new OperationNotSupportedException("Do not support composing composite names");
    }

    @Override
    public Object addToEnvironment(final String propName, final Object propVal) throws NamingException {
        if (environnement == null) {
            environnement = new Hashtable<String, Object>();
        }
        return environnement.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(final String propName) throws NamingException {
        if (environnement == null) {
            return null;
        }
        return environnement.remove(propName);
    }

    @Override
    public Hashtable<String, Object> getEnvironment() throws NamingException {
        final Hashtable<String, Object> result = new Hashtable<String, Object>();
        if (environnement != null) {
            result.putAll(environnement);
        }
        return result;
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        NamingContext ancestor = parent;
        // No ancestor
        if (ancestor == null) {
            return "";
        }
        final Name name = NamingContext.nameParser.parse("");
        name.add(myAtomicName);
        // Get parent's names
        while ((ancestor != null) && (ancestor.myAtomicName != null)) {
            name.add(0, ancestor.myAtomicName);
            ancestor = ancestor.parent;
        }

        return name.toString();
    }

    @Override
    public String toString() {
        if (myAtomicName != null) {
            return myAtomicName;
        } else {
            return "ROOT CONTEXT";
        }
    }

    @Override
    public void close() throws NamingException {
    }

}
