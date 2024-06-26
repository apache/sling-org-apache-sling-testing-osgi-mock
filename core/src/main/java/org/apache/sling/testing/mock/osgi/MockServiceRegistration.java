/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.osgi;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.framework.FilterImpl;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Mock {@link ServiceRegistration} implementation.
 */
class MockServiceRegistration<T> implements ServiceRegistration<T>, Comparable<MockServiceRegistration<T>> {

    private static final AtomicLong SERVICE_ID_COUNTER = new AtomicLong();

    private final Long serviceId;
    private final Set<String> clazzes;
    private final T service;
    private final Hashtable<String, Object> properties;
    private final ServiceReference<T> serviceReference;
    private final MockBundleContext bundleContext;

    @SuppressWarnings("unchecked")
    public MockServiceRegistration(
            final Bundle bundle,
            final String[] clazzes,
            final T service,
            final Dictionary<String, Object> properties,
            MockBundleContext bundleContext) {
        this.serviceId = SERVICE_ID_COUNTER.incrementAndGet();
        this.clazzes = new HashSet<String>(Arrays.asList(clazzes));

        if (service instanceof ServiceFactory) {
            this.service = ((ServiceFactory<T>) service).getService(bundleContext.getBundle(), this);
        } else {
            this.service = service;
        }

        readOsgiMetadata();

        this.properties = new Hashtable<>();
        this.updateProperties(properties);
        this.serviceReference = new MockServiceReference<T>(bundle, this);
        this.bundleContext = bundleContext;
    }

    private void updateProperties(final Dictionary<String, ?> newProps) {
        this.properties.clear();
        if (newProps != null) {
            final Enumeration<String> names = newProps.keys();
            while (names.hasMoreElements()) {
                final String key = names.nextElement();
                this.properties.put(key, newProps.get(key));
            }
        }
        this.properties.put(Constants.SERVICE_ID, this.serviceId);
        this.properties.put(Constants.OBJECTCLASS, this.clazzes.toArray(new String[this.clazzes.size()]));
    }

    @Override
    public ServiceReference<T> getReference() {
        return this.serviceReference;
    }

    @Override
    public void setProperties(final Dictionary<String, ?> newProps) {
        this.updateProperties(newProps);
        this.bundleContext.notifyServiceListeners(ServiceEvent.MODIFIED, this.serviceReference);
    }

    @Override
    public void unregister() {
        bundleContext.unregisterService(this);
    }

    Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    Map<String, Object> getPropertiesAsMap() {
        return MapUtil.toMap(this.properties);
    }

    boolean matches(final String clazz, final String filter) throws InvalidSyntaxException {
        return (clazz == null || this.clazzes.contains(clazz))
                && (filter == null || new FilterImpl(filter).match(properties));
    }

    Set<String> getClasses() {
        return clazzes;
    }

    T getService() {
        return this.service;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MockServiceRegistration)) {
            return false;
        }
        return serviceId.equals(((MockServiceRegistration) obj).serviceId);
    }

    @Override
    public int hashCode() {
        return serviceId.hashCode();
    }

    @Override
    public int compareTo(MockServiceRegistration<T> obj) {
        return serviceId.compareTo(obj.serviceId);
    }

    /**
     * Try to read OSGI-metadata from /OSGI-INF and read all implemented interfaces
     */
    @SuppressWarnings("null")
    private void readOsgiMetadata() {
        Class<?> serviceClass = service.getClass();
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(serviceClass);
        if (metadata == null) {
            return;
        }

        // add service interfaces from OSGi metadata - but only if no explicit class(es) were given on service
        // registration
        if (clazzes.isEmpty()) {
            clazzes.addAll(metadata.getServiceInterfaces());
        }
    }

    @SuppressWarnings("null")
    @Override
    public String toString() {
        return "#" + serviceId + " [" + StringUtils.join(clazzes, ",") + "]: " + service.toString();
    }
}
