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
package org.apache.sling.testing.mock.osgi.context;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.felix.scr.impl.inject.Annotations;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.MockEventAdmin;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.config.ComponentPropertyParser;
import org.apache.sling.testing.mock.osgi.config.AnnotationTypedConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

/**
 * Defines OSGi context objects and helper methods.
 * Should not be used directly but via the OsgiContext JUnit rule or extension.
 */
@ConsumerType
public class OsgiContextImpl {

    protected ComponentContext componentContext;

    /**
     * Setup actions before test method execution
     */
    protected void setUp() {
        registerDefaultServices();
    }

    /**
     * Teardown actions after test method execution
     */
    protected void tearDown() {
        if (componentContext != null) {
            // deactivate all services
            MockOsgi.shutdown(componentContext.getBundleContext());
        }

        this.componentContext = null;
    }

    /**
     * Default services that should be available for every unit test
     */
    private void registerDefaultServices() {
        registerInjectActivateService(new MockEventAdmin());
    }

    /**
     * @return OSGi component context
     */
    public final @NotNull ComponentContext componentContext() {
        if (this.componentContext == null) {
            this.componentContext = MockOsgi.newComponentContext();
        }
        return this.componentContext;
    }

    /**
     * @return OSGi Bundle context
     */
    public final @NotNull BundleContext bundleContext() {
        return componentContext().getBundleContext();
    }

    /**
     * Registers a service in the mocked OSGi environment.
     * @param <T> Service type
     * @param service Service instance
     * @return Registered service instance
     */
    public final @NotNull <T> T registerService(@NotNull final T service) {
        return registerService(null, service, (Map<String,Object>)null);
    }

    /**
     * Registers a service in the mocked OSGi environment.
     * @param <T> Service type
     * @param serviceClass Service class
     * @param service Service instance
     * @return Registered service instance
     */
    public final @NotNull <T> T registerService(@Nullable final Class<T> serviceClass, @NotNull final T service) {
        return registerService(serviceClass, service, (Map<String,Object>)null);
    }

    /**
     * Registers a service in the mocked OSGi environment.
     * @param <T> Service type
     * @param serviceClass Service class
     * @param service Service instance
     * @param properties Service properties (optional)
     * @return Registered service instance
     */
    public final @NotNull <T> T registerService(@Nullable final Class<T> serviceClass, @NotNull final T service, @Nullable final Map<String, Object> properties) {
        Dictionary<String, Object> serviceProperties = MapUtil.toDictionary(properties);
        bundleContext().registerService(serviceClass != null ? serviceClass.getName() : null, service, serviceProperties);
        return service;
    }

    /**
     * Registers a service in the mocked OSGi environment.
     * @param <T> Service type
     * @param serviceClass Service class
     * @param service Service instance
     * @param properties Service properties (optional)
     * @return Registered service instance
     */
    public final @NotNull <T> T registerService(@Nullable final Class<T> serviceClass, @NotNull final T service, @NotNull final Object @NotNull ... properties) {
        return registerService(serviceClass, service, MapUtil.toMap(properties));
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * Construction injection for OSGi services is supported.
     * @param <T> DS Component type
     * @param component a DS component instance
     * @return the DS component instance
     */
    public final @NotNull <T> T registerInjectActivateService(@NotNull final T component) {
        return registerInjectActivateService(component, (Map<String,Object>)null);
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * Constructor injection for DS components is supported.
     * @param <T> DS Component type
     * @param component a DS component instance
     * @param properties component properties (optional)
     * @return the DS component instance
     */
    public final @NotNull <T> T registerInjectActivateService(@NotNull final T component, @Nullable final Map<String, Object> properties) {
        MockOsgi.registerInjectActivateService(component, bundleContext(), properties);
        return component;
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * Constructor injection for DS components is supported.
     * @param <T> DS Component type
     * @param component a DS component instance
     * @param properties component properties (optional)
     * @return the DS component instance
     */
    public final @NotNull <T> T registerInjectActivateService(@NotNull final T component, @NotNull final Object @NotNull ... properties) {
        return registerInjectActivateService(component, MapUtil.toMap(properties));
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * Constructor injection for DS components is supported.
     * @param <T> DS Component type
     * @param componentClass a DS component class
     * @return the DS component instance
     */
    public final @NotNull <T> T registerInjectActivateService(@NotNull final Class<T> componentClass) {
        return registerInjectActivateService(componentClass, (Map<String,Object>)null);
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * Construction injection for OSGi services is supported.
     * @param <T> DS Component type
     * @param componentClass a DS component class
     * @param properties component properties (optional)
     * @return the DS component instance
     */
    public final @NotNull <T> T registerInjectActivateService(@NotNull Class<T> componentClass, @Nullable final Map<String, Object> properties) {
        return MockOsgi.registerInjectActivateService(componentClass, bundleContext(), properties);
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * @param <T> DS Component type
     * @param componentClass a DS component class
     * @param properties component properties (optional)
     * @return the DS component instance
     */
    public final @NotNull <T> T registerInjectActivateService(@NotNull Class<T> componentClass, @NotNull final Object @NotNull ... properties) {
        return registerInjectActivateService(componentClass, MapUtil.toMap(properties));
    }

    /**
     * Lookup a single service
     * @param <ServiceType> Service type
     * @param serviceType The type (interface) of the service.
     * @return The service instance, or null if the service is not available.
     */
    @SuppressWarnings("unchecked")
    public final @Nullable <ServiceType> ServiceType getService(@NotNull final Class<ServiceType> serviceType) {
        ServiceReference serviceReference = bundleContext().getServiceReference(serviceType.getName());
        if (serviceReference != null) {
            return (ServiceType)bundleContext().getService(serviceReference);
        } else {
            return null;
        }
    }

    /**
     * Lookup one or several services
     * @param <ServiceType> Service type
     * @param serviceType The type (interface) of the service.
     * @param filter An optional filter (LDAP-like, see OSGi spec)
     * @return The services instances or an empty array.
     * @throws RuntimeException If the <code>filter</code> string is not a valid OSGi service filter string.
     */
    @SuppressWarnings("unchecked")
    public final @NotNull <ServiceType> ServiceType @NotNull [] getServices(@NotNull final Class<ServiceType> serviceType, @Nullable final String filter) {
        try {
            ServiceReference[] serviceReferences = bundleContext().getServiceReferences(serviceType.getName(), filter);
            if (serviceReferences != null) {
                ServiceType[] services = (ServiceType[])Array.newInstance(serviceType, serviceReferences.length);
                for (int i = 0; i < serviceReferences.length; i++) {
                    services[i] = (ServiceType)bundleContext().getService(serviceReferences[i]);
                }
                return services;
            } else {
                return (ServiceType[])Array.newInstance(serviceType, 0);
            }
        } catch (InvalidSyntaxException ex) {
            throw new RuntimeException("Invalid filter syntax: " + filter, ex);
        }
    }

    /**
     * Return a concrete instance of the OSGi config / Component Property Type represented by the given
     * {@link ApplyConfig} annotation discovered via reflection.
     * @param annotation the {@link ApplyConfig}
     * @return a concrete instance of the type specified by the provided {@link ApplyConfig#value()}
     */
    public final Object applyConfigToType(@NotNull final ApplyConfig annotation) {
        return applyConfigToType(annotation, null);
    }

    /**
     * Return a concrete instance of the OSGi config / Component Property Type represented by the given
     * {@link ApplyConfig} annotation discovered via reflection.
     * @param annotation the {@link ApplyConfig}
     * @param applyPid if not empty, override any specified {@link ApplyConfig#pid()}.
     * @return a concrete instance of the type specified by the provided {@link ApplyConfig#value()}
     */
    public final Object applyConfigToType(@NotNull final ApplyConfig annotation,
                                          @Nullable final String applyPid) {
        if (!annotation.value().isAnnotation() && !annotation.value().isInterface()) {
            throw new IllegalArgumentException("illegal value for ApplyConfig " + annotation.value());
        }
        final Map<String, Object> merged = new HashMap<>(
                ComponentPropertyParser.parse(annotation.value(), annotation.property()));
        Optional.ofNullable(applyPid).filter(pid -> !pid.isEmpty())
                .or(() -> Optional.of(annotation.pid()).filter(pid -> !pid.isEmpty()))
                .ifPresent(pid -> mergePropertiesFromConfigPid(merged, pid, this.getService(ConfigurationAdmin.class)));
        return Annotations.toObject(annotation.value(), merged, bundleContext().getBundle(), false);
    }

    /**
     * Internal utility method to merge properties from the specified configuration into the provided map.
     * @param pid the configuration pid
     * @param configurationAdmin a config admin service
     * @param mergedProperties a *mutable* map
     * @throws RuntimeException if an IOException is thrown by {@link ConfigurationAdmin#getConfiguration(String)}
     * @throws UnsupportedOperationException if an immutable map is passed
     */
    static void mergePropertiesFromConfigPid(@NotNull Map<String, Object> mergedProperties,
                                             @NotNull String pid,
                                             @Nullable ConfigurationAdmin configurationAdmin) {
        if (configurationAdmin != null) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(pid);
                Optional.ofNullable(MapUtil.toMap(configuration.getProperties())).ifPresent(mergedProperties::putAll);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read config for pid " + pid, e);
            }
        }
    }

    /**
     * Construct a collection typed config for the provided annotation.
     * @param annotation a component property type annotation or {@link ApplyConfig} annotation
     * @return a typed config
     */
    public final TypedConfig<?> newTypedConfig(@NotNull final Annotation annotation) {
        return newTypedConfig(annotation, null);
    }

    /**
     * Construct a collection typed config for the provided annotation.
     * @param annotation a component property type annotation or {@link ApplyConfig} annotation
     * @param applyPid optional non-empty configuration pid to apply if annotation is a {@link ApplyConfig}
     * @return a typed config
     */
    public final TypedConfig<?> newTypedConfig(@NotNull final Annotation annotation,
                                               @Nullable final String applyPid) {
        if (annotation instanceof ApplyConfig) {
            ApplyConfig osgiConfig = (ApplyConfig) annotation;
            Class<?> mappingType = osgiConfig.value();
            return AnnotationTypedConfig.newInstance(mappingType,
                    mappingType.cast(this.applyConfigToType(osgiConfig, applyPid)),
                    annotation);
        } else {
            return AnnotationTypedConfig.newInstance(annotation.annotationType(), annotation, annotation);
        }
    }
}
