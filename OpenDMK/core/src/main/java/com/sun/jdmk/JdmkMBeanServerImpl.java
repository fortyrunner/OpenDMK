/*
 * @(#)file      JdmkMBeanServerImpl.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.61
 * @(#)date      07/10/01
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jdmk;

// java import

import com.sun.jdmk.interceptor.CompatibleMBeanInterceptor;
import com.sun.jdmk.interceptor.DefaultMBeanServerInterceptor;
import com.sun.jdmk.interceptor.MBeanServerInterceptor;

import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import java.io.ObjectInputStream;
import java.util.Set;

// RI import

/**
 * <p>Java DMK internal implementation of MBeanServer.</p>
 * <p>
 * Instance of this classes can be obtained from the
 * {@link  MBeanServerFactory} after setting the system property
 * <tt>javax.management.builder.initial</tt> value to point to the
 * Java DMK MBeanServerBuilder {@link JdmkMBeanServerBuilder
 * com.sun.jdmk.JdmkMBeanServerBuilder}.
 * <p>
 * This is the base class for MBean manipulation on the agent side. It
 * contains the methods necessary for the creation, registration, and
 * deletion of MBeans as well as the access methods for registered MBeans.
 * This is the core component of the JMX infrastructure.
 * <p>
 * Every MBean which is added to the MBean server becomes manageable:
 * its attributes and operations become remotely accessible through
 * the connectors/adaptors connected to that MBean server.
 * A Java object cannot be registered in the MBean server unless it is a
 * JMX compliant MBean.
 * <p>
 * When an MBean is registered or unregistered in the MBean server an
 * {@link javax.management.MBeanServerNotification MBeanServerNotification}
 * Notification is emitted. To register an object as listener to
 * MBeanServerNotifications you should call the MBean server method
 * {@link #addNotificationListener addNotificationListener} with
 * the <CODE>ObjectName</CODE> of the
 * {@link javax.management.MBeanServerDelegate MBeanServerDelegate}.
 * This <CODE>ObjectName</CODE> is:
 * <BR>
 * <CODE>JMImplementation:type=MBeanServerDelegate</CODE>.
 *
 * @since Java DMK 5.1
 */
final class JdmkMBeanServerImpl
  implements MBeanServerInterceptor, JdmkMBeanServer,
  MBeanServerInt, ProxyHandler {

  /**
   * The name of this class to be used for tracing
   */
  private final String dbgTag = "JdmkMBeanServerImpl";

  /**
   * Revisit: kept for backward compatibility. Should we remove it ?
   **/
  private MBeanInstantiator instantiator = null;

  /**
   * Revisit: kept for backward compatibility. Should we remove it ?
   **/
  private MetaData meta = null;

  /**
   * Revisit: transient ???
   **/
  private final transient MBeanServer outerMBeanServer;

  /**
   * Revisit: transient ???
   **/
  private transient MBeanServerInterceptor mbsInterceptor = null;

  /**
   * Revisit: transient ???
   **/
  private transient MBeanServer innerMBeanServer = null;

  /** Revisit: transient ??? **/
  /**
   * The MBeanServerDelegate object representing the MBean Server
   */
  private final transient MBeanServerDelegate mbsDelegate;

  /**
   * <b>Public internal:</b> Creates an MBeanServer with the
   * specified default domain name, outer interface, and delegate.
   * <p>This constructor is used internally
   * by the {@link com.sun.jdmk.JdmkMBeanServerBuilder}.
   * <p>The default domain name is used as the domain part in the ObjectName
   * of MBeans if no domain is specified by the user.
   * <ul><b>Note:</b>Using this constructor directly is strongly
   *     discouraged. You should use
   *     {@link javax.management.MBeanServerFactory#createMBeanServer(java.lang.String)}
   *     or
   *     {@link javax.management.MBeanServerFactory#newMBeanServer(java.lang.String)}
   *     instead.
   * </ul>
   *
   * @param domain   The default domain name used by this MBeanServer.
   * @param outer    A pointer to the MBeanServer object that must be
   *                 passed to the MBeans when invoking their
   *                 {@link javax.management.MBeanRegistration} interface.
   * @param delegate A pointer to the MBeanServerDelegate associated
   *                 with the new MBeanServer. The new MBeanServer must register
   *                 this MBean in its MBean repository.
   * @throws IllegalArgumentException if the instantiator is null.
   */
  JdmkMBeanServerImpl(String domain, MBeanServer outer,
                      MBeanServerDelegate delegate) {
    if (delegate == null) {
      throw new
        IllegalArgumentException("MBeanServerDelegate is null");
    }

    if (outer == null) {
      outer = this;
    }

    this.mbsDelegate = delegate;
    this.outerMBeanServer = outer;
  }

  /**
   * <b>Public internal:</b> Initialize the JdmkMbeanServer.
   * <p>This method is called internally
   * by the {@link com.sun.jdmk.JdmkMBeanServerBuilder}.
   * It must be called once, before starting to use this object.
   **/
  void initialize(MBeanServer inner) {
    this.innerMBeanServer = inner;
    if (innerMBeanServer == null) {
      throw new
        IllegalStateException("JdmkMBeanServer not initialized");
    }
    this.mbsInterceptor = new
      DefaultMBeanServerInterceptor(outerMBeanServer, mbsDelegate, inner);
  }

  @Override
  public MBeanServer getOuterMBeanServer() {
    return outerMBeanServer;
  }

  @Override
  public synchronized MBeanInterceptor getDefaultMBeanInterceptor() {
    if (mbsInterceptor == null) {
      return null;
    }
    if (mbsInterceptor instanceof CompatibleMBeanInterceptor) {
      return ((CompatibleMBeanInterceptor) mbsInterceptor).
        getNextInterceptor();
    }
    return mbsInterceptor;
  }

  @Override
  public synchronized void
  setDefaultMBeanInterceptor(MBeanInterceptor interceptor)
    throws IllegalArgumentException {
    if (interceptor == null) {
      throw new IllegalArgumentException("Null MBeanInterceptor");
    }
    if (interceptor instanceof MBeanServerInterceptor) {
      setMBeanServerInterceptor((MBeanServerInterceptor) interceptor);
    } else {
      setMBeanServerInterceptor(new CompatibleMBeanInterceptor(interceptor));
    }
  }

  @Override
  public synchronized MBeanServerInterceptor getMBeanServerInterceptor() {
    return mbsInterceptor;
  }

  @Override
  public synchronized void
  setMBeanServerInterceptor(MBeanServerInterceptor interceptor)
    throws IllegalArgumentException {
    if (interceptor == null) {
      throw new IllegalArgumentException("Null MBeanServerInterceptor");
    }
    this.mbsInterceptor = interceptor;
  }

  /**
   * Return the MBeanInstantiator associated to this MBeanServer.
   *
   * @deprecated Use one of the public constructor in
   * {@link com.sun.jdmk.MBeanInstantiatorImpl}
   */
  @Override
  public MBeanInstantiator getMBeanInstantiator() {
    if (innerMBeanServer == null) {
      throw new
        IllegalStateException("JdmkMBeanServer not initialized");
    }
    if (instantiator == null) {
      final ModifiableClassLoaderRepository clr =
        new CompatibleClassLoaderRepositorySupport(innerMBeanServer);
      instantiator = new MBeanInstantiatorImpl(clr);
    }
    return instantiator;
  }

  /**
   * Return the MetaData associated to this MBeanServer.
   *
   * @deprecated Use one of the public constructor in
   * {@link com.sun.jdmk.MetaDataImpl}
   */
  @Override
  public MetaData getMetaData() {
    if (meta == null) {
      final MBeanInstantiator mbsi = getMBeanInstantiator();
      meta = new MetaDataImpl(mbsi);
    }
    return meta;
  }

  /**
   * Instantiates and registers an MBean in the MBean server.
   * The MBean server will use its
   * {@link javax.management.loading.ClassLoaderRepository Default Loader Repository}
   * to load the class of the MBean.
   * An object name is associated to the MBean.
   * If the object name given is null, the MBean can automatically
   * provide its own name by implementing the
   * {@link javax.management.MBeanRegistration MBeanRegistration} interface.
   * The call returns an <CODE>ObjectInstance</CODE> object representing
   * the newly created MBean.
   *
   * @param className The class name of the MBean to be instantiated.
   * @param name      The object name of the MBean. May be null.
   * @return An <CODE>ObjectInstance</CODE>, containing the
   * <CODE>ObjectName</CODE> and the Java class name of the newly
   * instantiated MBean.
   * @throws ReflectionException            Wraps an
   *                                        <CODE>{@link java.lang.ClassNotFoundException}</CODE> or an
   *                                        <CODE>{@link java.lang.Exception}</CODE> that occurred
   *                                        when trying to invoke the MBean's constructor.
   * @throws InstanceAlreadyExistsException The MBean is already
   *                                        under the control of the MBean server.
   * @throws MBeanRegistrationException     The <CODE>preRegister()</CODE>
   *                                        (<CODE>MBeanRegistration</CODE> interface) method of the MBean
   *                                        has thrown an exception. The MBean will not be registered.
   * @throws MBeanException                 The constructor of the MBean has thrown
   *                                        an exception.
   * @throws NotCompliantMBeanException     This class is not a JMX
   *                                        compliant MBean.
   * @throws RuntimeOperationsException     Wraps an
   *                                        <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
   *                                        The className passed in parameter is null, the
   *                                        <CODE>ObjectName</CODE> passed in parameter contains a pattern
   *                                        or no <CODE>ObjectName</CODE> is specified for the MBean.
   */
  @Override
  public ObjectInstance createMBean(String className, ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException,
    NotCompliantMBeanException {

    return mbsInterceptor.createMBean(className, name, null,
      null);
  }

  /**
   * Instantiates and registers an MBean in the MBean server.
   * The class loader to be used is identified by its object  name.
   * An object name is associated to the MBean.
   * If the object name  of the loader is null, the ClassLoader that
   * loaded the MBean server will be used.
   * If the MBean's object name given is null, the MBean can
   * automatically provide its own name by implementing the
   * {@link javax.management.MBeanRegistration MBeanRegistration} interface.
   * The call returns an <CODE>ObjectInstance</CODE> object representing
   * the newly created MBean.
   *
   * @param className  The class name of the MBean to be instantiated.
   * @param name       The object name of the MBean. May be null.
   * @param loaderName The object name of the class loader to be used.
   * @return An <CODE>ObjectInstance</CODE>, containing the
   * <CODE>ObjectName</CODE> and the Java class name
   * of the newly instantiated MBean.
   * @throws ReflectionException            Wraps an
   *                                        <CODE>{@link java.lang.ClassNotFoundException}</CODE> or an
   *                                        <CODE>{@link java.lang.Exception}</CODE> that occurred when trying
   *                                        to invoke the MBean's constructor.
   * @throws InstanceAlreadyExistsException The MBean is already
   *                                        under the control of the MBean server.
   * @throws MBeanRegistrationException     The <CODE>preRegister()</CODE>
   *                                        (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
   *                                        has thrown an exception. The MBean will not be registered.
   * @throws MBeanException                 The constructor of the MBean has thrown
   *                                        an exception
   * @throws NotCompliantMBeanException     This class is not a JMX
   *                                        compliant MBean.
   * @throws InstanceNotFoundException      The specified class loader
   *                                        is not registered in the MBean server.
   * @throws RuntimeOperationsException     Wraps an
   *                                        <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
   *                                        className passed in parameter is null, the <CODE>ObjectName</CODE>
   *                                        passed in parameter contains a pattern or no
   *                                        <CODE>ObjectName</CODE> is specified for the MBean.
   */
  @Override
  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException,
    NotCompliantMBeanException, InstanceNotFoundException {

    return mbsInterceptor.createMBean(className, name, loaderName,
      null, null);
  }


  /**
   * Instantiates and registers an MBean in the MBean server.
   * The MBean server will use its
   * {@link javax.management.loading.ClassLoaderRepository Default Loader Repository}
   * to load the class of the MBean.
   * An object name is associated to the MBean.
   * If the object name given is null, the MBean can automatically
   * provide its own name by implementing the
   * {@link javax.management.MBeanRegistration MBeanRegistration} interface.
   * The call returns an <CODE>ObjectInstance</CODE> object representing
   * the newly created MBean.
   *
   * @param className The class name of the MBean to be instantiated.
   * @param name      The object name of the MBean. May be null.
   * @param params    An array containing the parameters of the constructor
   *                  to be invoked.
   * @param signature An array containing the signature of the
   *                  constructor to be invoked.
   * @return An <CODE>ObjectInstance</CODE>, containing the
   * <CODE>ObjectName</CODE> and the Java class name
   * of the newly instantiated MBean.
   * @throws ReflectionException            Wraps a
   *                                        <CODE>{@link java.lang.ClassNotFoundException}</CODE> or an
   *                                        <CODE>{@link java.lang.Exception}</CODE> that occurred
   *                                        when trying to invoke the MBean's constructor.
   * @throws InstanceAlreadyExistsException The MBean is already
   *                                        under the control of the MBean server.
   * @throws MBeanRegistrationException     The <CODE>preRegister()</CODE>
   *                                        (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
   *                                        has thrown an exception. The MBean will not be registered.
   * @throws MBeanException                 The constructor of the MBean has
   *                                        thrown an exception.
   * @throws RuntimeOperationsException     Wraps an
   *                                        <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
   *                                        className passed in parameter is null, the <CODE>ObjectName</CODE>
   *                                        passed in parameter contains a pattern or no
   *                                        <CODE>ObjectName</CODE> is specified for the MBean.
   */
  @Override
  public ObjectInstance createMBean(String className, ObjectName name,
                                    Object params[], String signature[])
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException,
    NotCompliantMBeanException {

    return mbsInterceptor.createMBean(className, name, params, signature);
  }

  /**
   * Instantiates and registers an MBean in the MBean server.
   * The class loader to be used is identified by its object name.
   * An object name is associated to the MBean. If the object name
   * of the loader is not specified, the ClassLoader that loaded the
   * MBean server will be used.
   * If  the MBean object name given is null, the MBean can automatically
   * provide its own name by implementing the
   * {@link javax.management.MBeanRegistration MBeanRegistration} interface.
   * The call returns an <CODE>ObjectInstance</CODE> object representing
   * the newly created MBean.
   *
   * @param className  The class name of the MBean to be instantiated.
   * @param name       The object name of the MBean. May be null.
   * @param params     An array containing the parameters of the constructor
   *                   to be invoked.
   * @param signature  An array containing the signature of the
   *                   constructor to be invoked.
   * @param loaderName The object name of the class loader to be used.
   * @return An <CODE>ObjectInstance</CODE>, containing the
   * <CODE>ObjectName</CODE> and the Java class name of the newly
   * instantiated MBean.
   * @throws ReflectionException            Wraps a
   *                                        <CODE>{@link java.lang.ClassNotFoundException}</CODE> or an
   *                                        <CODE>{@link java.lang.Exception}</CODE>
   *                                        that occurred when trying to invoke the MBean's constructor.
   * @throws InstanceAlreadyExistsException The MBean is already
   *                                        under the control of the MBean server.
   * @throws MBeanRegistrationException     The <CODE>preRegister()</CODE>
   *                                        (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
   *                                        has thrown an exception. The MBean will not be registered.
   * @throws MBeanException                 The constructor of the MBean has
   *                                        thrown an exception
   * @throws InstanceNotFoundException      The specified class loader is
   *                                        not registered in the MBean server.
   * @throws RuntimeOperationsException     Wraps an
   *                                        <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
   *                                        className passed in parameter is null, the <CODE>ObjectName</CODE>
   *                                        passed in parameter contains a pattern or no
   *                                        <CODE>ObjectName</CODE> is specified for the MBean.
   */
  @Override
  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName, Object params[],
                                    String signature[])
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException,
    NotCompliantMBeanException, InstanceNotFoundException {

    return mbsInterceptor.createMBean(className, name, loaderName,
      params, signature);
  }

  /**
   * Registers a pre-existing object as an MBean with the MBean server.
   * If the object name given is null, the MBean may automatically
   * provide its own name by implementing the
   * {@link javax.management.MBeanRegistration MBeanRegistration}  interface.
   * The call returns an <CODE>ObjectInstance</CODE> object representing
   * the registered MBean.
   *
   * @param object The  MBean to be registered as an MBean.
   * @param name   The object name of the MBean. May be null.
   * @return The <CODE>ObjectInstance</CODE> for the MBean that has been
   * registered.
   * @throws InstanceAlreadyExistsException The MBean is already
   *                                        under the control of the MBean server.
   * @throws MBeanRegistrationException     The <CODE>preRegister()</CODE>
   *                                        (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
   *                                        has thrown an exception. The MBean will not be registered.
   * @throws NotCompliantMBeanException     This object is not a JMX
   *                                        compliant MBean
   * @throws RuntimeOperationsException     Wraps an
   *                                        <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
   *                                        object passed in parameter is null or no object name is specified.
   */
  @Override
  public ObjectInstance registerMBean(Object object, ObjectName name)
    throws InstanceAlreadyExistsException, MBeanRegistrationException,
    NotCompliantMBeanException {

    return mbsInterceptor.registerMBean(object, name);
  }

  /**
   * De-registers an MBean from the MBean server. The MBean is identified by
   * its object name. Once the method has been invoked, the MBean may
   * no longer be accessed by its object name.
   *
   * @param name The object name of the MBean to be de-registered.
   * @throws InstanceNotFoundException  The MBean specified is not
   *                                    registered in the MBean server.
   * @throws MBeanRegistrationException The <code>preDeregister()<code>
   *                                    (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
   *                                    has thrown an exception.
   * @throws RuntimeOperationsException Wraps an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
   *                                    object name in parameter is null or the MBean you are when
   *                                    trying to de-register is the
   *                                    {@link javax.management.MBeanServerDelegate MBeanServerDelegate}
   *                                    MBean.
   **/
  @Override
  public void unregisterMBean(ObjectName name)
    throws InstanceNotFoundException, MBeanRegistrationException {
    // Now handled by the delegate itself..
    // if (name.equals(MBeanServerDelegateObjectName)) {
    //    throw new RuntimeOperationsException(
    //          new IllegalArgumentException(
    //               "The MBeanDelegate MBean cannot be unregistered"));
    // }
    mbsInterceptor.unregisterMBean(name);
  }

  /**
   * Gets the <CODE>ObjectInstance</CODE> for a given MBean registered
   * with the MBean server.
   *
   * @param name The object name of the MBean.
   * @return The <CODE>ObjectInstance</CODE> associated to the MBean
   * specified by <VAR>name</VAR>.
   * @throws InstanceNotFoundException The MBean specified is not
   *                                   registered in the MBean server.
   */
  @Override
  public ObjectInstance getObjectInstance(ObjectName name)
    throws InstanceNotFoundException {

    return mbsInterceptor.getObjectInstance(name);
  }

  /**
   * Gets MBeans controlled by the MBean server. This method allows any
   * of the following to be obtained: All MBeans, a set of MBeans specified
   * by pattern matching on the <CODE>ObjectName</CODE> and/or a Query
   * expression, a specific MBean. When the object name is null or no
   * domain and key properties are specified, all objects are to be
   * selected (and filtered if a query is specified). It returns the
   * set of <CODE>ObjectInstance</CODE> objects (containing the
   * <CODE>ObjectName</CODE> and the Java Class name) for
   * the selected MBeans.
   *
   * @param name  The object name pattern identifying the MBeans to
   *              be retrieved. If null or or no domain and key properties
   *              are specified, all the MBeans registered will be retrieved.
   * @param query The query expression to be applied for selecting
   *              MBeans. If null no query expression will be applied for
   *              selecting MBeans.
   * @return A set containing the <CODE>ObjectInstance</CODE> objects
   * for the selected MBeans.
   * If no MBean satisfies the query an empty list is returned.
   */
  @Override
  public Set queryMBeans(ObjectName name, QueryExp query) {

    return mbsInterceptor.queryMBeans(name, query);
  }

  /**
   * Gets the names of MBeans controlled by the MBean server. This method
   * enables any of the following to be obtained: The names of all MBeans,
   * the names of a set of MBeans specified by pattern matching on the
   * <CODE>ObjectName</CODE> and/or a Query expression, a specific
   * MBean name (equivalent to testing whether an MBean is registered).
   * When the object name is null or or no domain and key properties are
   * specified, all objects are selected (and filtered if a query is
   * specified). It returns the set of ObjectNames for the MBeans
   * selected.
   *
   * @param name  The object name pattern identifying the MBeans to be
   *              retrieved. If null or no domain and key properties are
   *              specified, all the MBeans registered will be retrieved.
   * @param query The query expression to be applied for selecting
   *              MBeans. If null no query expression will be applied for
   *              selecting MBeans.
   * @return A set containing the ObjectNames for the MBeans selected.
   * If no MBean satisfies the query, an empty list is returned.
   */
  @Override
  public Set queryNames(ObjectName name, QueryExp query) {

    return mbsInterceptor.queryNames(name, query);
  }


  /**
   * Checks whether an MBean, identified by its object name, is already
   * registered with the MBean server.
   *
   * @param name The object name of the MBean to be checked.
   * @return True if the MBean is already registered in the MBean server,
   * false otherwise.
   * @throws RuntimeOperationsException Wraps an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The object
   *                                    name in parameter is null.
   */
  @Override
  public boolean isRegistered(ObjectName name) {

    return mbsInterceptor.isRegistered(name);
  }


  /**
   * Returns the number of MBeans registered in the MBean server.
   */
  @Override
  public Integer getMBeanCount() {

    return mbsInterceptor.getMBeanCount();
  }

  /**
   * Gets the value of a specific attribute of a named MBean. The MBean
   * is identified by its object name.
   *
   * @param name      The object name of the MBean from which the attribute
   *                  is to be retrieved.
   * @param attribute A String specifying the name of the attribute to be
   *                  retrieved.
   * @return The value of the retrieved attribute.
   * @throws AttributeNotFoundException The attribute specified
   *                                    is not accessible in the MBean.
   * @throws MBeanException             Wraps an exception thrown by the
   *                                    MBean's getter.
   * @throws InstanceNotFoundException  The MBean specified is not
   *                                    registered in the MBean server.
   * @throws ReflectionException        Wraps an
   *                                    <CODE>{@link java.lang.Exception}</CODE> thrown when trying to
   *                                    invoke the setter.
   * @throws RuntimeOperationsException Wraps an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
   *                                    The object name in parameter is null or the attribute in
   *                                    parameter is null.
   */
  @Override
  public Object getAttribute(ObjectName name, String attribute)
    throws MBeanException, AttributeNotFoundException,
    InstanceNotFoundException, ReflectionException {

    return mbsInterceptor.getAttribute(name, attribute);
  }


  /**
   * Enables the values of several attributes of a named MBean. The MBean
   * is identified by its object name.
   *
   * @param name       The object name of the MBean from which the attributes are
   *                   retrieved.
   * @param attributes A list of the attributes to be retrieved.
   * @return The list of the retrieved attributes.
   * @throws InstanceNotFoundException  The MBean specified is not
   *                                    registered in the MBean server.
   * @throws ReflectionException        An exception occurred when trying
   *                                    to invoke the getAttributes method of a Dynamic MBean.
   * @throws RuntimeOperationsException Wrap an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
   *                                    object name in parameter is null or attributes in parameter
   *                                    is null.
   */
  @Override
  public AttributeList getAttributes(ObjectName name, String[] attributes)
    throws InstanceNotFoundException, ReflectionException {

    return mbsInterceptor.getAttributes(name, attributes);

  }

  /**
   * Sets the value of a specific attribute of a named MBean. The MBean
   * is identified by its object name.
   *
   * @param name      The name of the MBean within which the attribute is
   *                  to be set.
   * @param attribute The identification of the attribute to be set
   *                  and the value it is to be set to.
   * @return The value of the attribute that has been set.
   * @throws InstanceNotFoundException      The MBean specified is
   *                                        not registered in the MBean server.
   * @throws AttributeNotFoundException     The attribute specified is
   *                                        not accessible in the MBean.
   * @throws InvalidAttributeValueException The value specified for
   *                                        the attribute is not valid.
   * @throws MBeanException                 Wraps an exception thrown by the
   *                                        MBean's setter.
   * @throws ReflectionException            Wraps an
   *                                        <CODE>{@link java.lang.Exception}</CODE> thrown when trying
   *                                        to invoke the setter.
   * @throws RuntimeOperationsException     Wraps an
   *                                        <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
   *                                        object name in parameter is null or the attribute in parameter
   *                                        is null.
   */
  @Override
  public void setAttribute(ObjectName name, Attribute attribute)
    throws InstanceNotFoundException, AttributeNotFoundException,
    InvalidAttributeValueException, MBeanException,
    ReflectionException {

    mbsInterceptor.setAttribute(name, attribute);
  }


  /**
   * Sets the values of several attributes of a named MBean. The MBean is
   * identified by its object name.
   *
   * @param name       The object name of the MBean within which the
   *                   attributes are to  be set.
   * @param attributes A list of attributes: The identification of the
   *                   attributes to be set and  the values they are to be set to.
   * @return The list of attributes that were set, with their new values.
   * @throws InstanceNotFoundException  The MBean specified is not
   *                                    registered in the MBean server.
   * @throws ReflectionException        An exception occurred when trying
   *                                    to invoke the getAttributes method of a Dynamic MBean.
   * @throws RuntimeOperationsException Wraps an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
   *                                    The object name in parameter is null or  attributes in
   *                                    parameter is null.
   */
  @Override
  public AttributeList setAttributes(ObjectName name,
                                     AttributeList attributes)
    throws InstanceNotFoundException, ReflectionException {

    return mbsInterceptor.setAttributes(name, attributes);
  }

  /**
   * Invokes an operation on an MBean.
   *
   * @param name          The object name of the MBean on which the method is to be
   *                      invoked.
   * @param operationName The name of the operation to be invoked.
   * @param params        An array containing the parameters to be set when
   *                      the operation is invoked
   * @param signature     An array containing the signature of the operation.
   *                      The class objects will be loaded using the same class loader as
   *                      the one used for loading the MBean on which the operation was
   *                      invoked.
   * @return The object returned by the operation, which represents the
   * result of invoking the operation on the  MBean specified.
   * @throws InstanceNotFoundException The MBean specified is not
   *                                   registered in the MBean server.
   * @throws MBeanException            Wraps an exception thrown by the MBean's
   *                                   invoked method.
   * @throws ReflectionException       Wraps an
   *                                   <CODE>{@link java.lang.Exception}</CODE> thrown while trying
   *                                   to invoke the method.
   */
  @Override
  public Object invoke(ObjectName name, String operationName,
                       Object params[], String signature[])
    throws InstanceNotFoundException, MBeanException,
    ReflectionException {
    return mbsInterceptor.invoke(name, operationName, params, signature);
  }

  /**
   * Returns the default domain used for naming the MBean.
   * The default domain name is used as the domain part in the ObjectName
   * of MBeans if no domain is specified by the user.
   */
  @Override
  public String getDefaultDomain() {
    return mbsInterceptor.getDefaultDomain();
  }


  @Override
  public String[] getDomains() {
    return mbsInterceptor.getDomains();
  }

  /**
   * Adds a listener to a registered MBean.
   *
   * @param name     The name of the MBean on which the listener should be added.
   * @param listener The listener object which will handle the
   *                 notifications emitted by the registered MBean.
   * @param filter   The filter object. If filter is null, no filtering
   *                 will be performed before handling notifications.
   * @param handback The context to be sent to the listener when a
   *                 notification is emitted.
   * @throws InstanceNotFoundException The MBean name provided does
   *                                   not match any of the registered MBeans.
   */
  @Override
  public void addNotificationListener(ObjectName name,
                                      NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException {

    mbsInterceptor.addNotificationListener(name, listener, filter, handback);
  }


  /**
   * Adds a listener to a registered MBean.
   *
   * @param name     The name of the MBean on which the listener should be added.
   * @param listener The object name of the listener which will handle the
   *                 notifications emitted by the registered MBean.
   * @param filter   The filter object. If filter is null, no filtering will
   *                 be performed before handling notifications.
   * @param handback The context to be sent to the listener when a
   *                 notification is emitted.
   * @throws InstanceNotFoundException The MBean name of the
   *                                   notification listener or of the notification broadcaster
   *                                   does not match any of the registered MBeans.
   */
  @Override
  public void addNotificationListener(ObjectName name, ObjectName listener,
                                      NotificationFilter filter, Object handback)
    throws InstanceNotFoundException {

    mbsInterceptor.addNotificationListener(name, listener, filter, handback);
  }

  @Override
  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener)
    throws InstanceNotFoundException, ListenerNotFoundException {

    mbsInterceptor.removeNotificationListener(name, listener);
  }

  @Override
  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException {

    mbsInterceptor.
      removeNotificationListener(name, listener, filter, handback);
  }

  @Override
  public void removeNotificationListener(ObjectName name,
                                         ObjectName listener)
    throws InstanceNotFoundException, ListenerNotFoundException {

    mbsInterceptor.removeNotificationListener(name, listener);
  }

  @Override
  public void removeNotificationListener(ObjectName name,
                                         ObjectName listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException {

    mbsInterceptor.
      removeNotificationListener(name, listener, filter, handback);
  }

  /**
   * This method discovers the attributes and operations that an MBean
   * exposes for management.
   *
   * @param name The name of the MBean to analyze
   * @return An instance of <CODE>MBeanInfo</CODE> allowing the
   * retrieval of all attributes and operations of this MBean.
   * @throws IntrospectionException    An exception occurs during
   *                                   introspection.
   * @throws InstanceNotFoundException The MBean specified is not found.
   * @throws ReflectionException       An exception occurred when trying to
   *                                   invoke the getMBeanInfo of a Dynamic MBean.
   */
  @Override
  public MBeanInfo getMBeanInfo(ObjectName name) throws
    InstanceNotFoundException, IntrospectionException, ReflectionException {

    return mbsInterceptor.getMBeanInfo(name);
  }

  /**
   * Instantiates an object using the list of all class loaders registered
   * in the MBean server (using its
   * {@link javax.management.loading.ClassLoaderRepository Default Loader Repository}).
   * The object's class should have a public constructor.
   * It returns a reference to the newly created object.
   * The newly created object is not registered in the MBean server.
   *
   * @param className The class name of the object to be instantiated.
   * @return The newly instantiated object.
   * @throws ReflectionException        Wraps the
   *                                    <CODE>{@link java.lang.ClassNotFoundException}</CODE> or the
   *                                    <CODE>{@link java.lang.Exception}</CODE> that
   *                                    occurred when trying to invoke the object's constructor.
   * @throws MBeanException             The constructor of the object has thrown
   *                                    an exception.
   * @throws RuntimeOperationsException Wraps an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
   *                                    The className passed in parameter is null.
   */
  @Override
  public Object instantiate(String className)
    throws ReflectionException, MBeanException {
    return innerMBeanServer.instantiate(className);
  }


  /**
   * Instantiates an object using the class Loader specified by its
   * <CODE>ObjectName</CODE>.
   * If the loader name is null, the ClassLoader that loaded the
   * MBean Server will be used.
   * The object's class should have a public constructor.
   * It returns a reference to the newly created object.
   * The newly created object is not registered in the MBean server.
   *
   * @param className  The class name of the MBean to be instantiated.
   * @param loaderName The object name of the class loader to be used.
   * @return The newly instantiated object.
   * @throws ReflectionException        Wraps the
   *                                    <CODE>{@link java.lang.ClassNotFoundException}</CODE> or the
   *                                    <CODE>{@link java.lang.Exception}</CODE> that
   *                                    occurred when trying to invoke the object's constructor.
   * @throws MBeanException             The constructor of the object has thrown
   *                                    an exception.
   * @throws InstanceNotFoundException  The specified class loader
   *                                    is not registered in the MBaenServer.
   * @throws RuntimeOperationsException Wraps an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
   *                                    className passed in parameter is null.
   */
  @Override
  public Object instantiate(String className, ObjectName loaderName)
    throws ReflectionException, MBeanException,
    InstanceNotFoundException {

    return innerMBeanServer.instantiate(className, loaderName);
  }

  /**
   * Instantiates an object using the list of all class loaders registered
   * in the MBean server (using its
   * {@link javax.management.loading.ClassLoaderRepository Default Loader Repository}).
   * The object's class should have a public constructor.
   * The call returns a reference to the newly created object.
   * The newly created object is not registered in the MBean server.
   *
   * @param className The class name of the object to be instantiated.
   * @param params    An array containing the parameters of the constructor
   *                  to be invoked.
   * @param signature An array containing the signature of the
   *                  constructor to be invoked.
   * @return The newly instantiated object.
   * @throws ReflectionException        Wraps the
   *                                    <CODE>{@link java.lang.ClassNotFoundException}</CODE> or the
   *                                    <CODE>{@link java.lang.Exception}</CODE> that
   *                                    occurred when trying to invoke the object's constructor.
   * @throws MBeanException             The constructor of the object has thrown
   *                                    an exception.
   * @throws RuntimeOperationsException Wraps an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
   *                                    The className passed in parameter is null.
   */
  @Override
  public Object instantiate(String className, Object params[],
                            String signature[])
    throws ReflectionException, MBeanException {

    return innerMBeanServer.instantiate(className, params, signature);

  }


  /**
   * Instantiates an object. The class loader to be used is identified
   * by its object name. If the object name of the loader is null,
   * the ClassLoader that loaded the MBean server will be used.
   * The object's class should have a public constructor.
   * The call returns a reference to the newly created object.
   * The newly created object is not registered in the MBean server.
   *
   * @param className  The class name of the object to be instantiated.
   * @param params     An array containing the parameters of the constructor
   *                   to be invoked.
   * @param signature  An array containing the signature of the constructor
   *                   to be invoked.
   * @param loaderName The object name of the class loader to be used.
   * @return The newly instantiated object.
   * @throws ReflectionException        Wraps the
   *                                    <CODE>{@link java.lang.ClassNotFoundException}</CODE> or the
   *                                    <CODE>{@link java.lang.Exception}</CODE> that
   *                                    occurred when trying to invoke the object's constructor.
   * @throws MBeanException             The constructor of the object has thrown
   *                                    an exception.
   * @throws InstanceNotFoundException  The specified class loader
   *                                    is not registered in the MBean server.
   * @throws RuntimeOperationsException Wraps an
   *                                    <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
   *                                    The className passed in parameter is null.
   */
  @Override
  public Object instantiate(String className, ObjectName loaderName,
                            Object params[], String signature[])
    throws ReflectionException, MBeanException,
    InstanceNotFoundException {

    return innerMBeanServer.instantiate(className, loaderName, params,
      signature);
  }


  /**
   * Returns true if the MBean specified is an instance of the specified
   * class, false otherwise.
   *
   * @param name      The <CODE>ObjectName</CODE> of the MBean.
   * @param className The name of the class.
   * @return true if the MBean specified is an instance of the specified
   * class, false otherwise.
   * @throws InstanceNotFoundException The MBean specified is not
   *                                   registered in the MBean server.
   */
  @Override
  public boolean isInstanceOf(ObjectName name, String className)
    throws InstanceNotFoundException {

    return mbsInterceptor.isInstanceOf(name, className);
  }

  /**
   * De-serializes a byte array in the context of the class loader
   * of an MBean.
   *
   * @param name The name of the MBean whose class loader should
   *             be used for the de-serialization.
   * @param data The byte array to be de-sererialized.
   * @return The de-serialized object stream.
   * @throws InstanceNotFoundException The MBean specified is not
   *                                   found.
   * @throws OperationsException       Any of the usual Input/Output
   *                                   related exceptions.
   */
  @Override
  public ObjectInputStream deserialize(ObjectName name, byte[] data)
    throws InstanceNotFoundException, OperationsException {

    return innerMBeanServer.deserialize(name, data);
  }


  /**
   * De-serializes a byte array in the context of a given MBean class loader.
   * The class loader is the one that loaded the class with name "className".
   *
   * @param className The name of the class whose class loader should be
   *                  used for the de-serialization.
   * @param data      The byte array to be de-sererialized.
   * @return The de-serialized object stream.
   * @throws OperationsException Any of the usual Input/Output
   *                             related exceptions.
   * @throws ReflectionException The specified class could not be
   *                             loaded by the default loader repository
   */
  @Override
  public ObjectInputStream deserialize(String className, byte[] data)
    throws OperationsException, ReflectionException {

    return innerMBeanServer.deserialize(className, data);
  }


  /**
   * De-serializes a byte array in the context of a given MBean class loader.
   * The class loader is the one that loaded the class with name "className".
   * The name of the class loader to be used for loading the specified
   * class is specified.
   * If null, the MBean Server's class loader will be used.
   *
   * @param className  The name of the class whose class loader should be
   *                   used for the de-serialization.
   * @param data       The byte array to be de-sererialized.
   * @param loaderName The name of the class loader to be used for
   *                   loading the specified class.
   *                   If null, the MBean Server's class loader will be used.
   * @return The de-serialized object stream.
   * @throws InstanceNotFoundException The specified class loader
   *                                   MBean is not found.
   * @throws OperationsException       Any of the usual Input/Output
   *                                   related exceptions.
   * @throws ReflectionException       The specified class could not
   *                                   be loaded by the specified class loader.
   */
  @Override
  public ObjectInputStream deserialize(String className,
                                       ObjectName loaderName, byte[] data)
    throws InstanceNotFoundException, OperationsException,
    ReflectionException {

    return innerMBeanServer.deserialize(className, loaderName, data);
  }

  @Override
  public javax.management.MBeanServerDelegate getMBeanServerDelegate() {
    return mbsDelegate;
  }

  @Override
  public final ClassLoader getMBeanClassLoader(ObjectName name)
    throws InstanceNotFoundException {
    return getClassLoaderFor(name);
  }

  @Override
  public ClassLoader getClassLoaderFor(ObjectName mbeanName)
    throws InstanceNotFoundException {
    return mbsInterceptor.getClassLoaderFor(mbeanName);
  }

  @Override
  public ClassLoader getClassLoader(ObjectName loaderName)
    throws InstanceNotFoundException {
    if (mbsInterceptor instanceof CompatibleMBeanInterceptor) {
      try {
        // Ok, first try with MBeanServer in that case...
        return innerMBeanServer.getClassLoader(loaderName);
      } catch (InstanceNotFoundException x) { /* try again... */ }
    }

    return mbsInterceptor.getClassLoader(loaderName);
  }

  @Override
  public ClassLoaderRepository getClassLoaderRepository() {
    return innerMBeanServer.getClassLoaderRepository();
  }

}
