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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.framework.FilterImpl;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.DynamicReference;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.Reference;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtil.ReferenceInfo;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtil.ServiceInfo;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;

/**
 * Mock {@link BundleContext} implementation.
 */
class MockBundleContext implements BundleContext {

    private final MockBundle bundle;
    private final SortedSet<MockServiceRegistration> registeredServices = new ConcurrentSkipListSet<MockServiceRegistration>();
    private final Map<ServiceListener, Filter> serviceListeners = new ConcurrentHashMap<ServiceListener, Filter>();
    private final Queue<BundleListener> bundleListeners = new ConcurrentLinkedQueue<BundleListener>();
    private final ConfigurationAdmin configAdmin = new MockConfigurationAdmin(this);
    private File dataFileBaseDir;

    private final Bundle systemBundle;

    public MockBundleContext() {
        this.systemBundle = new MockBundle(this, Constants.SYSTEM_BUNDLE_ID);
        this.bundle = new MockBundle(this);

        // register configuration admin by default
        registerService(ConfigurationAdmin.class.getName(), configAdmin, null);
    }

    @Override
    public Bundle getBundle() {
        return this.bundle;
    }

    @Override
    public Filter createFilter(final String s) throws InvalidSyntaxException {
        if (s == null) {
            return new MatchAllFilter();
        }
        else {
            return new FilterImpl(s);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceRegistration registerService(final String clazz, final Object service, final Dictionary properties) {
        String[] clazzes;
        if (StringUtils.isBlank(clazz)) {
            clazzes = new String[0];
        } else {
            clazzes = new String[] { clazz };
        }
        return registerService(clazzes, service, properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        return registerService(clazz.getName(), service, properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceRegistration registerService(final String[] clazzes, final Object service, final Dictionary properties) {
        Dictionary<String, Object> mergedPropertes = MapMergeUtil.propertiesMergeWithOsgiMetadata(service.getClass(), configAdmin, properties);
        MockServiceRegistration<?> registration = new MockServiceRegistration<>(this.bundle, clazzes, service, mergedPropertes, this);
        this.registeredServices.add(registration);
        handleRefsUpdateOnRegister(registration, this);
        notifyServiceListeners(ServiceEvent.REGISTERED, registration.getReference());
        return registration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory, Dictionary<String, ?> properties) {
        return registerService(clazz.getName(), factory, properties);
    }

    /**
     * Check for already registered services that may be affected by the service registration - either
     * adding by additional optional references, or creating a conflict in the dependencies.
     * @param registration Service registration
     * @param bundleContext Bundle context
     */
    @SuppressWarnings("unchecked")
    private void handleRefsUpdateOnRegister(MockServiceRegistration<?> registration, BundleContext bundleContext) {

        // handle DYNAMIC references to this registration
        List<ReferenceInfo<?>> affectedDynamicReferences = OsgiServiceUtil.getMatchingDynamicReferences(registeredServices, registration);
        for (ReferenceInfo<?> referenceInfo : affectedDynamicReferences) {
            Reference reference = referenceInfo.getReference();
            // Look for a target override
            Object o = referenceInfo.getServiceRegistration().getProperties().get(reference.getName() + ComponentConstants.REFERENCE_TARGET_SUFFIX);
            if (o instanceof String) {
                reference = new DynamicReference(reference,(String)o);
            }
            if (reference.matchesTargetFilter(registration.getReference())) {
                switch (reference.getCardinality()) {
                case MANDATORY_UNARY:
                    // nothing to do - reference is already set
                    break;
                case MANDATORY_MULTIPLE:
                case OPTIONAL_MULTIPLE:
                case OPTIONAL_UNARY:
                    OsgiServiceUtil.invokeBindMethod(reference, referenceInfo.getServiceRegistration().getService(),
                            new ServiceInfo(registration), bundleContext);
                    break;
                default:
                    throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
                }
            }
        }

        // handle STATIC+GREEDY references to this registration
        List<ReferenceInfo<?>> affectedStaticGreedyReferences = OsgiServiceUtil.getMatchingStaticGreedyReferences(registeredServices, registration);
        Set<MockServiceRegistration<?>> servicesToRestart = new HashSet<>();
        for (ReferenceInfo<?> referenceInfo : affectedStaticGreedyReferences) {
            Reference reference = referenceInfo.getReference();
            if (reference.matchesTargetFilter(registration.getReference())) {
                switch (reference.getCardinality()) {
                case MANDATORY_UNARY:
                    // nothing to do - reference is already set
                    break;
                case MANDATORY_MULTIPLE:
                case OPTIONAL_MULTIPLE:
                case OPTIONAL_UNARY:
                    servicesToRestart.add(referenceInfo.getServiceRegistration());
                    break;
                default:
                    throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
                }
            }
        }
        servicesToRestart.forEach(this::restartService);
    }

    void unregisterService(MockServiceRegistration<?> registration) {
        boolean wasRemoved = this.registeredServices.remove(registration);
        if (wasRemoved) {
            handleRefsUpdateOnUnregister(registration, this);
            notifyServiceListeners(ServiceEvent.UNREGISTERING, registration.getReference());
        } else {
            throw new IllegalStateException("Service was already unregistered");
        }
    }

    @SuppressWarnings("null")
    void restartService(@NotNull MockServiceRegistration<?> registration) {
        // get current service properties
        Class<?> serviceClass = registration.getService().getClass();
        Map<String,Object> properties = registration.getPropertiesAsMap();

        // deactivate & unregister service
        MockOsgi.deactivate(registration.getService(), this);
        unregisterService(registration);

        // newly create and register service
        Object newService = MockOsgi.activateInjectServices(serviceClass, this, properties);

        String[] serviceInterfaces = registration.getClasses().toArray(new String[registration.getClasses().size()]);
        registerService(serviceInterfaces, newService, MapUtil.toDictionary(properties));
    }

    /**
     * Check for already registered services that may be affected by the service unregistration - either
     * adding by removing optional references, or creating a conflict in the dependencies.
     * @param registration Service registration
     * @param bundleContext Bundle context
     */
    @SuppressWarnings("unchecked")
    private void handleRefsUpdateOnUnregister(MockServiceRegistration<?> registration, BundleContext bundleContext) {

        // handle DYNAMIC references to this registration
        List<ReferenceInfo<?>> affectedDynamicReferences = OsgiServiceUtil.getMatchingDynamicReferences(registeredServices, registration);
        for (ReferenceInfo<?> referenceInfo : affectedDynamicReferences) {
            Reference reference = referenceInfo.getReference();
            if (reference.matchesTargetFilter(registration.getReference())) {
                switch (reference.getCardinality()) {
                case MANDATORY_UNARY:
                case MANDATORY_MULTIPLE:
                case OPTIONAL_MULTIPLE:
                case OPTIONAL_UNARY:
                    // it is currently not checked if for a MANDATORY_UNARY or MANDATORY_MULTIPLE reference the last reference is removed
                    OsgiServiceUtil.invokeUnbindMethod(reference, referenceInfo.getServiceRegistration().getService(),
                            new ServiceInfo(registration), bundleContext);
                    break;
                default:
                    throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
                }
            }
        }

        // handle STATIC+GREEDY references to this registration
        List<ReferenceInfo<?>> affectedStaticGreedyReferences = OsgiServiceUtil.getMatchingStaticGreedyReferences(registeredServices, registration);
        Set<MockServiceRegistration<?>> servicesToRestart = new HashSet<>();
        for (ReferenceInfo<?> referenceInfo : affectedStaticGreedyReferences) {
            Reference reference = referenceInfo.getReference();
            if (reference.matchesTargetFilter(registration.getReference())) {
                switch (reference.getCardinality()) {
                case MANDATORY_UNARY:
                case MANDATORY_MULTIPLE:
                case OPTIONAL_MULTIPLE:
                case OPTIONAL_UNARY:
                    servicesToRestart.add(referenceInfo.getServiceRegistration());
                    break;
                default:
                    throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
                }
            }
        }
        servicesToRestart.forEach(this::restartService);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceReference getServiceReference(final String clazz) {
        try {
            ServiceReference[] serviceRefs = getServiceReferences(clazz, null);
            if (serviceRefs != null && serviceRefs.length > 0) {
                return serviceRefs[0];
            }
        }
        catch (InvalidSyntaxException ex) {
            // should not happen
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        return getServiceReference(clazz.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceReference[] getServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException {
        /*
         * Please note that the OSGi spec does not declare any ordering for the getServiceReferences method
         * https://docs.osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.framework.BundleContext.getServiceReferences-String-String-
         * for backward compatibility with previous implementation of osgi-mock we stick with highest-ranking first here
         */
        Set<ServiceReference> result = new TreeSet<>(new Comparator<ServiceReference>() {
            @Override
            public int compare(ServiceReference o1, ServiceReference o2) {
                // reverse sort order to get highest ranking first
                return o2.compareTo(o1);
            }
        });
        for (MockServiceRegistration serviceRegistration : this.registeredServices) {
            if (serviceRegistration.matches(clazz, filter)) {
                result.add(serviceRegistration.getReference());
            }
        }
        if (result.isEmpty()) {
            return null;
        } else {
            return result.toArray(new ServiceReference[result.size()]);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
        ServiceReference<S>[] result = getServiceReferences(clazz.getName(), filter);
        if (result == null) {
            return Collections.emptyList();
        }
        else {
            return Arrays.asList(result);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceReference[] getAllServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException {
        // for now just do the same as getServiceReferences
        return getServiceReferences(clazz, filter);
    }

    @Override
    public <S> S getService(final ServiceReference<S> serviceReference) {
        return ((MockServiceReference<S>)serviceReference).getService();
    }

    @Override
    public boolean ungetService(final ServiceReference serviceReference) {
        // do nothing for now
        return false;
    }

    @Override
    public void addServiceListener(final ServiceListener serviceListener) {
        try {
            addServiceListener(serviceListener, null);
        }
        catch (InvalidSyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addServiceListener(final ServiceListener serviceListener, final String filter) throws InvalidSyntaxException {
        serviceListeners.put(serviceListener, createFilter(filter));
    }

    @Override
    public void removeServiceListener(final ServiceListener serviceListener) {
        serviceListeners.remove(serviceListener);
    }

    void notifyServiceListeners(int eventType, ServiceReference serviceReference) {
        final ServiceEvent event = new ServiceEvent(eventType, serviceReference);
        for ( Map.Entry<ServiceListener, Filter> entry : serviceListeners.entrySet()) {
            if ( entry.getValue() == null || entry.getValue().match(serviceReference)) {
                entry.getKey().serviceChanged(event);
            }
        }
    }

    @Override
    public void addBundleListener(final BundleListener bundleListener) {
        if (!bundleListeners.contains(bundleListener)) {
            bundleListeners.add(bundleListener);
        }
    }

    @Override
    public void removeBundleListener(final BundleListener bundleListener) {
        bundleListeners.remove(bundleListener);
    }

    void sendBundleEvent(BundleEvent bundleEvent) {
        for (BundleListener bundleListener : bundleListeners) {
            bundleListener.bundleChanged(bundleEvent);
        }
    }

    @Override
    public void addFrameworkListener(final FrameworkListener frameworkListener) {
        // accept method, but ignore it
    }

    @Override
    public void removeFrameworkListener(final FrameworkListener frameworkListener) {
        // accept method, but ignore it
    }

    @SuppressWarnings({ "unchecked", "null" })
    <S> S locateService(final String name, final ServiceReference<S> reference) {
        for (MockServiceRegistration<?> serviceRegistration : this.registeredServices) {
            if (serviceRegistration.getReference() == reference) {
                return (S)serviceRegistration.getService();
            }
        }
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        return new Bundle[0];
    }

    @Override
    public String getProperty(final String s) {
        // no mock implementation, simulate that no property is found and return null
        return null;
    }

    @Override
    public File getDataFile(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        synchronized (this) {
            if (dataFileBaseDir == null) {
                try {
                    dataFileBaseDir = Files.createTempDirectory("osgi-mock").toFile();
                } catch (IOException ex) {
                    throw new RuntimeException("Error creating temp. directory.", ex);
                }
            }
        }
        if (path.isEmpty()) {
            return dataFileBaseDir;
        }
        else {
            return new File(dataFileBaseDir, path);
        }
    }

    /**
     * Deactivates all bundles registered in this mocked bundle context.
     */
    @SuppressWarnings("null")
    public void shutdown() {
        List<MockServiceRegistration> reversedRegisteredServices = new ArrayList<>(registeredServices);
        Collections.reverse(reversedRegisteredServices);
        for (MockServiceRegistration<?> serviceRegistration : reversedRegisteredServices) {
            try {
                MockOsgi.deactivate(serviceRegistration.getService(), this, serviceRegistration.getProperties());
            }
            catch (NoScrMetadataException ex) {
                // ignore, no deactivate method is available then
            }
        }
        if (dataFileBaseDir != null) {
            try {
                FileUtils.deleteDirectory(dataFileBaseDir);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public Bundle getBundle(final long bundleId) {
        if (bundleId == Constants.SYSTEM_BUNDLE_ID) {
            return systemBundle;
        }
        // otherwise return null - no bundle found
        return null;
    }

    @Override
    public Bundle getBundle(String location) {
        if (StringUtils.equals(location, Constants.SYSTEM_BUNDLE_LOCATION)) {
            return systemBundle;
        }
        // otherwise return null - no bundle found
        return null;
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
        return new ServiceObjects<S>() {
            @Override
            public S getService() {
                return MockBundleContext.this.getService(reference);
            }
            @Override
            public void ungetService(S service) {
                MockBundleContext.this.ungetService(reference);
            }
            @Override
            public ServiceReference<S> getServiceReference() {
                return reference;
            }
        };
    }


    // --- unsupported operations ---
    @Override
    public Bundle installBundle(final String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle installBundle(final String s, final InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

}
