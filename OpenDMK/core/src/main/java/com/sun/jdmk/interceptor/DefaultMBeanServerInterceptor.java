
/*
 * @(#)file      DefaultMBeanServerInterceptor.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.33
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

package com.sun.jdmk.interceptor;

// java import

import javax.management.*;
import java.util.Set;

// JMX import


/**
 * This is the default class for MBean manipulation on the agent side. It
 * contains the methods necessary for the creation, registration, and
 * deletion of MBeans as well as the access methods for registered MBeans.
 * This is the core component of the JMX infrastructure.
 * <p>
 * Every MBean which is added to the MBean server becomes manageable: its attributes and operations
 * become remotely accessible through the connectors/adaptors connected to that MBean server.
 * A Java object cannot be registered in the MBean server unless it is a JMX compliant MBean.
 * <p>
 * When an MBean is registered or unregistered in the MBean server an
 * {@link javax.management.MBeanServerNotification MBeanServerNotification}
 * Notification is emitted. To register an object as listener to MBeanServerNotifications
 * you should call the MBean server method {@link #addNotificationListener addNotificationListener} with <CODE>ObjectName</CODE>
 * the <CODE>ObjectName</CODE> of the {@link javax.management.MBeanServerDelegate MBeanServerDelegate}.
 * This <CODE>ObjectName</CODE> is:
 * <BR>
 * <CODE>JMImplementation:type=MBeanServerDelegate</CODE>.
 *
 * @since Java DMK 5.1
 */
public final class DefaultMBeanServerInterceptor
  implements MBeanServerInterceptor {

  /**
   * The inner MBean server object that associated to the
   * DefaultMBeanServerInterceptor
   */
  private final transient MBeanServer inner;

  /**
   * The name of this class to be used for tracing
   */
  private final static String dbgTag = "DefaultMBeanServerInterceptor";


  /**
   * Creates a DefaultMBeanServerInterceptor with the specified
   * default domain name.
   * The default domain name is used as the domain part in the ObjectName
   * of MBeans if no domain is specified by the user.
   * <p>Do not forget to call <code>initialize(outer,delegate)</code>
   * before using this object.
   *
   * @param outer    A pointer to the MBeanServer object that must be
   *                 passed to the MBeans when invoking their
   *                 {@link javax.management.MBeanRegistration} interface.
   * @param delegate A pointer to the MBeanServerDelegate associated
   *                 with the new MBeanServer. The new MBeanServer must register
   *                 this MBean in its MBean repository.
   * @param inner    A pointer to the inner MBeanServer object to which
   *                 this DefaultMBeanServerInterceptor is forwarding the calls.
   */
  public DefaultMBeanServerInterceptor(MBeanServer outer,
                                       MBeanServerDelegate delegate,
                                       MBeanServer inner) {
    if (inner == null) {
      throw new
        IllegalArgumentException("inner MBeanServer cannot be null");
    }
    this.inner = inner;
  }

  public ObjectInstance createMBean(String className, ObjectName name,
                                    Object params[], String signature[])
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanException,
    NotCompliantMBeanException {
    return inner.createMBean(className, name, params, signature);
  }

  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName, Object params[],
                                    String signature[])
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanException,
    NotCompliantMBeanException, InstanceNotFoundException {
    return inner.createMBean(className, name, loaderName, params,
      signature);
  }

  public ObjectInstance registerMBean(Object object, ObjectName name)
    throws InstanceAlreadyExistsException, MBeanRegistrationException,
    NotCompliantMBeanException {
    return inner.registerMBean(object, name);
  }

  public void unregisterMBean(ObjectName name)
    throws InstanceNotFoundException, MBeanRegistrationException {
    inner.unregisterMBean(name);
  }

  public ObjectInstance getObjectInstance(ObjectName name)
    throws InstanceNotFoundException {
    return inner.getObjectInstance(name);
  }

  public Set queryMBeans(ObjectName name, QueryExp query) {
    return inner.queryMBeans(name, query);
  }

  public Set queryNames(ObjectName name, QueryExp query) {
    return inner.queryNames(name, query);
  }

  public boolean isRegistered(ObjectName name) {
    return inner.isRegistered(name);
  }

  public Integer getMBeanCount() {
    return inner.getMBeanCount();
  }

  public Object getAttribute(ObjectName name, String attribute)
    throws MBeanException, AttributeNotFoundException,
    InstanceNotFoundException, ReflectionException {
    return inner.getAttribute(name, attribute);
  }

  public AttributeList getAttributes(ObjectName name, String[] attributes)
    throws InstanceNotFoundException, ReflectionException {
    return inner.getAttributes(name, attributes);
  }

  public void setAttribute(ObjectName name, Attribute attribute)
    throws InstanceNotFoundException, AttributeNotFoundException,
    InvalidAttributeValueException, MBeanException,
    ReflectionException {
    inner.setAttribute(name, attribute);
  }

  public AttributeList setAttributes(ObjectName name,
                                     AttributeList attributes)
    throws InstanceNotFoundException, ReflectionException {
    return inner.setAttributes(name, attributes);
  }

  public Object invoke(ObjectName name, String operationName,
                       Object params[], String signature[])
    throws InstanceNotFoundException, MBeanException,
    ReflectionException {
    return inner.invoke(name, operationName, params, signature);
  }

  public String getDefaultDomain() {
    return inner.getDefaultDomain();
  }

  public String[] getDomains() {
    return inner.getDomains();
  }

  public void addNotificationListener(ObjectName name,
                                      NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException {
    inner.addNotificationListener(name, listener, filter, handback);
  }

  public void addNotificationListener(ObjectName name,
                                      ObjectName listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException {
    inner.addNotificationListener(name, listener, filter, handback);
  }

  public void removeNotificationListener(ObjectName name,
                                         ObjectName listener)
    throws InstanceNotFoundException, ListenerNotFoundException {
    inner.removeNotificationListener(name, listener);
  }

  public void removeNotificationListener(ObjectName name,
                                         ObjectName listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException {
    inner.removeNotificationListener(name, listener, filter, handback);
  }

  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener)
    throws InstanceNotFoundException, ListenerNotFoundException {
    inner.removeNotificationListener(name, listener);
  }

  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException {
    inner.removeNotificationListener(name, listener, filter, handback);
  }

  public MBeanInfo getMBeanInfo(ObjectName name)
    throws InstanceNotFoundException, IntrospectionException,
    ReflectionException {
    return inner.getMBeanInfo(name);
  }

  public boolean isInstanceOf(ObjectName name, String className)
    throws InstanceNotFoundException {
    return inner.isInstanceOf(name, className);
  }

  /**
   * @deprecated Use
   * {@link #getClassLoaderFor(javax.management.ObjectName) }
   **/
  public final ClassLoader getMBeanClassLoader(ObjectName name)
    throws InstanceNotFoundException {
    return getClassLoaderFor(name);
  }

  public ClassLoader getClassLoader(ObjectName loaderName)
    throws InstanceNotFoundException {
    return inner.getClassLoader(loaderName);
  }

  public ClassLoader getClassLoaderFor(ObjectName mbeanName)
    throws InstanceNotFoundException {
    return inner.getClassLoaderFor(mbeanName);
  }

}
