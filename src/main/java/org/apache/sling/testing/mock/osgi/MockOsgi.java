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

import static org.apache.sling.testing.mock.osgi.MapMergeUtil.propertiesMergeWithOsgiMetadata;
import static org.apache.sling.testing.mock.osgi.MapUtil.toDictionary;
import static org.apache.sling.testing.mock.osgi.MapUtil.toMap;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

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
    public static @NotNull ComponentContext newComponentContext(@NotNull BundleContext bundleContext,
            @Nullable Dictionary<String, Object> properties) {
        return componentContext().bundleContext(bundleContext).properties(properties).build();
    }

    /**
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext(@NotNull BundleContext bundleContext,
            @Nullable Map<String, Object> properties) {
        return componentContext().bundleContext(bundleContext).properties(properties).build();
    }

    /**
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static @NotNull ComponentContext newComponentContext(@NotNull BundleContext bundleContext,
            @NotNull Object @NotNull ... properties) {
        return componentContext().bundleContext(bundleContext).properties(properties).build();
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
     * Simulate OSGi service dependency injection. Injects direct references and
     * multiple references. If a some references could not be injected no error
     * is thrown.
     * @param target Service instance
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @return true if all dependencies could be injected, false if the service has no dependencies.
     */
    public static boolean injectServices(@NotNull Object target, @NotNull BundleContext bundleContext) {
        return MockOsgi.injectServices(target, bundleContext, (Map<String, Object>)null);
    }

    /**
     * Simulate OSGi service dependency injection. Injects direct references and
     * multiple references. If a some references could not be injected no error
     * is thrown.
     * @param target Service instance
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @param properties Service properties (used to resolve dynamic reference properties)
     * @return true if all dependencies could be injected, false if the service has no dependencies.
     */
    public static boolean injectServices(@NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        return OsgiServiceUtil.injectServices(target, bundleContext, properties);
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(@NotNull Object target, @NotNull BundleContext bundleContext) {
        return MockOsgi.activate(target, bundleContext, (Dictionary<String, Object>)null);
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(@NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.activateDeactivate(target, componentContext, true);
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(@NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        return activate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(@NotNull Object target, @NotNull BundleContext bundleContext, @NotNull Object @NotNull ... properties) {
        return activate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulate deactivation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context.
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(@NotNull Object target, @NotNull BundleContext bundleContext) {
        return MockOsgi.deactivate(target, bundleContext, (Dictionary<String, Object>)null);
    }

    /**
     * Simulate deactivation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(@NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.activateDeactivate(target, componentContext, false);
    }

    /**
     * Simulate activation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(@NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        return deactivate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulate activation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(@NotNull Object target, @NotNull BundleContext bundleContext, @NotNull Object @NotNull ... properties) {
        return deactivate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulate configuration modification of service instance. Invokes the @Modified annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(@NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Dictionary<String, Object> properties) {
        return modified(target, bundleContext, toMap(properties));
    }

    /**
     * Simulate configuration modification of service instance. Invokes the @Modified annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(@NotNull Object target, @NotNull BundleContext bundleContext, @Nullable Map<String, Object> properties) {
        Map<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.modified(target, componentContext, mergedProperties);
    }
    
    /**
     * Simulate configuration modification of service instance. Invokes the @Modified annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(@NotNull Object target, @NotNull BundleContext bundleContext, @NotNull Object @NotNull ... properties) {
        return modified(target, bundleContext, toDictionary(properties));
    }
    
    /**
     * Set configuration via ConfigurationAdmin service in bundle context for component with given pid.
     * @param bundleContext Bundle context
     * @param pid PID
     * @param properties Configuration properties
     */
    public static void setConfigForPid(@NotNull BundleContext bundleContext, @NotNull String pid, @Nullable Map<String,Object> properties) {
        setConfigForPid(bundleContext, pid, toDictionary(properties));
    }
    
    /**
     * Set configuration via ConfigurationAdmin service in bundle context for component with given pid.
     * @param bundleContext Bundle context
     * @param pid PID
     * @param properties Configuration properties
     */
    public static void setConfigForPid(@NotNull BundleContext bundleContext, @NotNull String pid, @NotNull Object @NotNull ... properties) {
        setConfigForPid(bundleContext, pid, toDictionary(properties));
    }
    
    private static void setConfigForPid(@NotNull BundleContext bundleContext, @NotNull String pid, @Nullable Dictionary<String, Object> properties) {
        ConfigurationAdmin configAdmin = getConfigAdmin(bundleContext);
        if (configAdmin == null) {
            throw new RuntimeException("ConfigurationAdmin service is not registered in bundle context.");
        }
        try {
            Configuration config = configAdmin.getConfiguration(pid);
            config.update(properties);
        }
        catch (IOException ex) {
            throw new RuntimeException("Unable to update configuration for pid '" + pid + "'.", ex);
        }
    }
    
    /**
     * Deactivates all bundles registered in the mocked bundle context.
     * @param bundleContext Bundle context
     */
    public static void shutdown(@NotNull BundleContext bundleContext) {
        ((MockBundleContext)bundleContext).shutdown();
    }
    
    /**
     * Get configuration admin.
     * @param bundleContext Bundle context
     * @return Configuration admin or null if not registered.
     */
    private static @Nullable ConfigurationAdmin getConfigAdmin(@NotNull BundleContext bundleContext) {
        ServiceReference<?> ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (ref != null) {
            return (ConfigurationAdmin)bundleContext.getService(ref);
        }
        return null;
    }
    
}
