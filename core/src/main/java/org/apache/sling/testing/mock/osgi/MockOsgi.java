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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import static org.apache.sling.testing.mock.osgi.MapMergeUtil.propertiesMergeWithOsgiMetadata;
import static org.apache.sling.testing.mock.osgi.MapUtil.toDictionary;
import static org.apache.sling.testing.mock.osgi.MapUtil.toMap;

/**
 * Factory for mock OSGi objects.
 */
public final class MockOsgi {

    private MockOsgi() {
        // static methods only
    }

    /**
     * @return Mocked {@link BundleContext} instance
     */
    public static @NotNull BundleContext newBundleContext() {
        return new MockBundleContext();
    }

    /**
     * Simulates a bundle event on the given bundle context (that is forwarded
     * to registered bundle listeners).
     * @param bundleContext Bundle context
     * @param bundleEvent Bundle event
     */
    public static void sendBundleEvent(@NotNull BundleContext bundleContext, @NotNull BundleEvent bundleEvent) {
        ((MockBundleContext) bundleContext).sendBundleEvent(bundleEvent);
    }

    /**
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext() {
        return componentContext().build();
    }

    /**
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext(@Nullable Dictionary<String, Object> properties) {
        return componentContext().properties(properties).build();
    }

    /**
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext(@Nullable Map<String, Object> properties) {
        return componentContext().properties(properties).build();
    }

    /**
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext(@NotNull Object @NotNull ... properties) {
        return componentContext().properties(properties).build();
    }

    /**
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext(
            @NotNull BundleContext bundleContext, @Nullable Dictionary<String, Object> properties) {
        return componentContext()
                .bundleContext(bundleContext)
                .properties(properties)
                .build();
    }

    /**
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext(
            @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        return componentContext()
                .bundleContext(bundleContext)
                .properties(properties)
                .build();
    }

    /**
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext(
            @NotNull BundleContext bundleContext, @NotNull Object @NotNull ... properties) {
        return componentContext()
                .bundleContext(bundleContext)
                .properties(properties)
                .build();
    }

    /**
     * @return {@link ComponentContextBuilder} to build a mocked {@link ComponentContext}
     */
    public static @NotNull ComponentContextBuilder componentContext() {
        return new ComponentContextBuilder();
    }

    /**
     * @param loggerContext Context class for logging
     * @return Mocked {@link LogService} instance
     */
    public static @NotNull LogService newLogService(@NotNull final Class<?> loggerContext) {
        return new MockLogService(loggerContext);
    }

    /**
     * Simulates OSGi DS dependency injection. Injects direct references and multiple references.
     * If a some references could not be injected no error is thrown.
     * @param target Service instance
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @return true if all dependencies could be injected, false if the service has no dependencies.
     */
    public static boolean injectServices(@NotNull Object target, @NotNull BundleContext bundleContext) {
        return MockOsgi.injectServices(target, bundleContext, (Map<String, Object>) null);
    }

    /**
     * Simulates OSGi DS dependency injection. Injects direct references and multiple references.
     * If a some references could not be injected no error is thrown.
     * @param target Service instance
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @param properties Service properties (used to resolve dynamic reference properties)
     * @return true if all dependencies could be injected, false if the service has no dependencies.
     */
    public static boolean injectServices(
            @NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        return OsgiServiceUtil.injectServices(target, bundleContext, properties);
    }

    /**
     * Simulates OSGi DS dependency injection and activation. Injects direct references and multiple references.
     * If a some references could not be injected no error is thrown.
     * This method instantiates the service instance and also supports constructor injection.
     * @param targetClass Component/service class
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @param <T> Target class type
     * @return Component/service instances with injected services
     */
    public static @NotNull <T> T activateInjectServices(
            @NotNull Class<T> targetClass, @NotNull BundleContext bundleContext) {
        return MockOsgi.activateInjectServices(targetClass, bundleContext, (Map<String, Object>) null);
    }

    /**
     * Simulates OSGi DS dependency injection and activation. Injects direct references and multiple references.
     * If a some references could not be injected no error is thrown.
     * This method instantiates the service instance and also supports constructor injection.
     * @param targetClass Component/service class
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @param properties Service properties (used to resolve dynamic reference properties)
     * @param <T> Target class type
     * @return Component/service instances with injected services
     */
    public static @NotNull <T> T activateInjectServices(
            @NotNull Class<T> targetClass,
            @NotNull BundleContext bundleContext,
            @Nullable Map<String, Object> properties) {
        Map<String, Object> mergedProperties =
                propertiesMergeWithOsgiMetadata(targetClass, getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.activateInjectServices(targetClass, (MockComponentContext) componentContext);
    }

    /**
     * Simulates OSGi DS dependency injection and activation. Injects direct references and multiple references.
     * If a some references could not be injected no error is thrown.
     * This method instantiates the service instance and also supports constructor injection.
     * @param targetClass Component/service class
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @param properties Service properties (used to resolve dynamic reference properties)
     * @param <T> Target class type
     * @return Component/service instances with injected services
     */
    public static @NotNull <T> T activateInjectServices(
            @NotNull Class<T> targetClass,
            @NotNull BundleContext bundleContext,
            @NotNull Object @NotNull ... properties) {
        return activateInjectServices(targetClass, bundleContext, MapUtil.toMap(properties));
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * @param <T> DS Component type
     * @param component a DS component instance
     * @param bundleContext Bundle context from which services are fetched to inject and which is used for registering new services
     */
    public static final @NotNull <T> void registerInjectActivateService(
            @NotNull final T component, @NotNull BundleContext bundleContext) {
        registerInjectActivateService(component, bundleContext, (Map<String, Object>) null);
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * @param <T> DS Component type
     * @param component a DS component instance
     * @param bundleContext Bundle context from which services are fetched to inject and which is used for registering new services
     * @param properties component properties (optional)
     */
    public static final @NotNull <T> void registerInjectActivateService(
            @NotNull final T component,
            @NotNull BundleContext bundleContext,
            @Nullable final Map<String, Object> properties) {
        Map<String, Object> mergedProperties =
                propertiesMergeWithOsgiMetadata(component.getClass(), getConfigAdmin(bundleContext), properties);
        MockOsgi.injectServices(component, bundleContext, mergedProperties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        OsgiServiceUtil.activateDeactivate(component, (MockComponentContext) componentContext, true);
        registerDSComponent(component, bundleContext, mergedProperties);
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * @param <T> DS Component type
     * @param component a DS component instance
     * @param bundleContext Bundle context from which services are fetched to inject and which is used for registering new services.
     * @param properties component properties (optional)
     */
    public static final @NotNull <T> void registerInjectActivateService(
            @NotNull final T component,
            @NotNull BundleContext bundleContext,
            @NotNull final Object @NotNull ... properties) {
        registerInjectActivateService(component, bundleContext, MapUtil.toMap(properties));
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * @param <T> DS component type
     * @param dsComponentClass DS component class
     * @param bundleContext Bundle context from which services are fetched to inject and which is used for registering new services
     * @return Registered component instance
     */
    public static final @NotNull <T> T registerInjectActivateService(
            @NotNull final Class<T> dsComponentClass, @NotNull BundleContext bundleContext) {
        return registerInjectActivateService(dsComponentClass, bundleContext, (Map<String, Object>) null);
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * @param <T> DS component type
     * @param dsComponentClass DS component class
     * @param bundleContext Bundle context from which services are fetched to inject and which is used for registering new services
     * @param properties component properties (optional)
     * @return Registered component instance
     */
    public static final @NotNull <T> T registerInjectActivateService(
            @NotNull Class<T> dsComponentClass,
            @NotNull BundleContext bundleContext,
            @Nullable final Map<String, Object> properties) {
        Map<String, Object> mergedProperties =
                propertiesMergeWithOsgiMetadata(dsComponentClass, getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        T component = OsgiServiceUtil.activateInjectServices(dsComponentClass, (MockComponentContext) componentContext);
        registerDSComponent(component, bundleContext, mergedProperties);
        return component;
    }

    private static <T> void registerDSComponent(
            @NotNull T component, @NotNull BundleContext bundleContext, Map<String, Object> mergedProperties) {
        OsgiMetadata metadata = Objects.requireNonNull(
                OsgiMetadataUtil.getMetadata(component.getClass()), "No metadata found for " + component.getClass());

        // convert component properties to service properties
        // (http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-service.properties)
        Dictionary<String, Object> serviceProperties = mergedProperties.entrySet().stream()
                .filter(e -> e.getKey() != null && !e.getKey().startsWith("."))
                .collect(new DictionaryCollector<>(Entry::getKey, Entry::getValue));

        // we also register DS Components that aren't services in order for bind/unbind to work - they are registered
        // with no service interfaces
        bundleContext.registerService(
                metadata.getServiceInterfaces().toArray(new String[0]), component, serviceProperties);
    }

    /**
     * Injects dependencies, activates and registers a DS component in the mocked OSGi environment.
     * @param <T> DS component type
     * @param dsComponentClass DS component class
     * @param bundleContext Bundle context from which services are fetched to inject and which is used for registering new services
     * @param properties component properties (optional)
     * @return Registered component instance
     */
    public static final @NotNull <T> T registerInjectActivateService(
            @NotNull Class<T> dsComponentClass,
            @NotNull BundleContext bundleContext,
            @NotNull final Object @NotNull ... properties) {
        return registerInjectActivateService(dsComponentClass, bundleContext, MapUtil.toMap(properties));
    }

    /**
     * Simulates activation of a DS component instance. Invokes the @Activate annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(@NotNull Object target, @NotNull BundleContext bundleContext) {
        return MockOsgi.activate(target, bundleContext, (Dictionary<String, Object>) null);
    }

    /**
     * Simulates activation of a DS component instance. Invokes the @Activate annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(
            @NotNull Object target,
            @NotNull BundleContext bundleContext,
            @Nullable Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties =
                propertiesMergeWithOsgiMetadata(target.getClass(), getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.activateDeactivate(target, (MockComponentContext) componentContext, true);
    }

    /**
     * Simulates activation of a DS component instance. Invokes the @Activate annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(
            @NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        return activate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulates activation of a DS component instance. Invokes the @Activate annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(
            @NotNull Object target, @NotNull BundleContext bundleContext, @NotNull Object @NotNull ... properties) {
        return activate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulates deactivation of a DS component instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context.
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(@NotNull Object target, @NotNull BundleContext bundleContext) {
        return MockOsgi.deactivate(target, bundleContext, (Dictionary<String, Object>) null);
    }

    /**
     * Simulates deactivation of a DS component instance. Invokes the @Deactivate annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(
            @NotNull Object target,
            @NotNull BundleContext bundleContext,
            @Nullable Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties =
                propertiesMergeWithOsgiMetadata(target.getClass(), getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.activateDeactivate(target, (MockComponentContext) componentContext, false);
    }

    /**
     * Simulates deactivation of a DS component instance. Invokes the @Deactivate annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(
            @NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        return deactivate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulates deactivation of a DS component instance. Invokes the @Deactivate annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(
            @NotNull Object target, @NotNull BundleContext bundleContext, @NotNull Object @NotNull ... properties) {
        return deactivate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulates configuration modification of a DS component instance. Invokes the @Modified annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(
            @NotNull Object target,
            @NotNull BundleContext bundleContext,
            @Nullable Dictionary<String, Object> properties) {
        return modified(target, bundleContext, toMap(properties));
    }

    /**
     * Simulates configuration modification of a DS component instance. Invokes the @Modified annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(
            @NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        Map<String, Object> mergedProperties =
                propertiesMergeWithOsgiMetadata(target.getClass(), getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.modified(target, (MockComponentContext) componentContext, mergedProperties);
    }

    /**
     * Simulates configuration modification of a DS component instance. Invokes the @Modified annotated method.
     * @param target DS component instance
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(
            @NotNull Object target, @NotNull BundleContext bundleContext, @NotNull Object @NotNull ... properties) {
        return modified(target, bundleContext, toDictionary(properties));
    }

    /**
     * Set configuration via ConfigurationAdmin service in bundle context for component with given pid.
     * @param bundleContext Bundle context
     * @param pid PID
     * @param properties Configuration properties
     */
    public static void setConfigForPid(
            @NotNull BundleContext bundleContext, @NotNull String pid, @Nullable Map<String, Object> properties) {
        setConfigForPid(bundleContext, pid, toDictionary(properties));
    }

    /**
     * Set configuration via ConfigurationAdmin service in bundle context for component with given pid.
     * @param bundleContext Bundle context
     * @param pid PID
     * @param properties Configuration properties
     */
    public static void setConfigForPid(
            @NotNull BundleContext bundleContext, @NotNull String pid, @NotNull Object @NotNull ... properties) {
        setConfigForPid(bundleContext, pid, toDictionary(properties));
    }

    private static void setConfigForPid(
            @NotNull BundleContext bundleContext,
            @NotNull String pid,
            @Nullable Dictionary<String, Object> properties) {
        ConfigurationAdmin configAdmin = getConfigAdmin(bundleContext);
        if (configAdmin == null) {
            throw new RuntimeException("ConfigurationAdmin service is not registered in bundle context.");
        }
        try {
            Configuration config = configAdmin.getConfiguration(pid);
            config.update(properties);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to update configuration for pid '" + pid + "'.", ex);
        }
    }

    /**
     * Set factory configuration via ConfigurationAdmin service in bundle context for component with given pid.
     * @param bundleContext Bundle context
     * @param factoryPid factory PID
     * @param name the name for Configuration
     * @param properties Configuration properties
     */
    public static void setFactoryConfigForPid(
            @NotNull BundleContext bundleContext,
            @NotNull String factoryPid,
            @NotNull String name,
            @Nullable Map<String, Object> properties) {
        setFactoryConfigForPid(bundleContext, factoryPid, name, toDictionary(properties));
    }

    /**
     * Set factory configuration via ConfigurationAdmin service in bundle context for component with given pid.
     * @param bundleContext Bundle context
     * @param factoryPid factory PID
     * @param name the name for Configuration
     * @param properties Configuration properties
     */
    public static void setFactoryConfigForPid(
            @NotNull BundleContext bundleContext,
            @NotNull String factoryPid,
            @NotNull String name,
            @NotNull Object @NotNull ... properties) {
        setFactoryConfigForPid(bundleContext, factoryPid, name, toDictionary(properties));
    }

    private static void setFactoryConfigForPid(
            @NotNull BundleContext bundleContext,
            @NotNull String factoryPid,
            @NotNull String name,
            @Nullable Dictionary<String, Object> properties) {
        ConfigurationAdmin configAdmin = getConfigAdmin(bundleContext);
        if (configAdmin == null) {
            throw new RuntimeException("ConfigurationAdmin service is not registered in bundle context.");
        }
        try {
            Configuration config = configAdmin.getFactoryConfiguration(factoryPid, name);
            config.update(properties);
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Unable to update factory configuration for factoryPid '" + factoryPid + " and name " + name + "'.",
                    ex);
        }
    }

    /**
     * Deactivates all bundles registered in the mocked bundle context.
     * @param bundleContext Bundle context
     */
    public static void shutdown(@NotNull BundleContext bundleContext) {
        ((MockBundleContext) bundleContext).shutdown();
    }

    /**
     * Gets configuration admin.
     * @param bundleContext Bundle context
     * @return Configuration admin or null if not registered.
     */
    private static @Nullable ConfigurationAdmin getConfigAdmin(@NotNull BundleContext bundleContext) {
        ServiceReference<?> ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (ref != null) {
            return (ConfigurationAdmin) bundleContext.getService(ref);
        }
        return null;
    }
}
