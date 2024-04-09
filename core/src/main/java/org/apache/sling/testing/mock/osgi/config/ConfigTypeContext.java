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
package org.apache.sling.testing.mock.osgi.config;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.felix.scr.impl.inject.internal.Annotations;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;

/**
 * Performs configuration management and component property type construction for {@link ConfigType} and
 * {@link SetConfig} annotations.
 */
public final class ConfigTypeContext {
    private final OsgiContextImpl osgiContext;

    public ConfigTypeContext(OsgiContextImpl osgiContext) {
        this.osgiContext = osgiContext;
    }

    /**
     * Internal utility method to update a configuration with properties from the provided map.
     *
     * @param pid                the configuration pid
     * @param configurationAdmin a config admin service
     * @param updatedProperties  a map of properties to update the configuration
     * @throws RuntimeException if an IOException is thrown by {@link ConfigurationAdmin}
     */
    static void updatePropertiesForConfigPid(
            @NotNull Map<String, Object> updatedProperties,
            @NotNull String pid,
            @Nullable ConfigurationAdmin configurationAdmin) {
        if (configurationAdmin != null) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(pid);
                configuration.update(MapUtil.toDictionary(updatedProperties));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read/write config for pid " + pid, e);
            }
        }
    }

    /**
     * Internal utility method to merge properties from the specified configuration into the provided map.
     *
     * @param pid                the configuration pid
     * @param configurationAdmin a config admin service
     * @param mergedProperties   a *mutable* map
     * @throws RuntimeException              if an IOException is thrown by {@link ConfigurationAdmin#getConfiguration(String)}
     * @throws UnsupportedOperationException if an immutable map is passed
     */
    static void mergePropertiesFromConfigPid(
            @NotNull Map<String, Object> mergedProperties,
            @NotNull String pid,
            @Nullable ConfigurationAdmin configurationAdmin) {
        if (configurationAdmin != null) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(pid);
                Optional.ofNullable(MapUtil.toMap(configuration.getProperties()))
                        .ifPresent(mergedProperties::putAll);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read config for pid " + pid, e);
            }
        }
    }

    /**
     * Construct a configuration pid for use with {@link ConfigurationAdmin#getConfiguration(String)}. If {@code pid}
     * is not empty and not equal to {@link Component#NAME} ("$"), return {@code pid}. Otherwise, return
     * {@code component.getName()}, except in the case of {@link Void}, in which case return {@code empty()}.
     *
     * @param pid       an explicit pid name, "$", or the empty string
     * @param component a class whose name to use when pid is "$", unless {@link Void}
     * @return a useful configuration pid or none
     */
    @SuppressWarnings("rawtypes")
    public Optional<String> getConfigurationPid(@NotNull final String pid, @NotNull final Class component) {
        if (pid.isEmpty() || Component.NAME.equals(pid)) {
            if (Void.class.equals(component)) {
                return Optional.empty();
            } else {
                return Optional.of(component.getName());
            }
        } else {
            return Optional.of(pid);
        }
    }

    /**
     * Updates a {@link Configuration} from the provided annotation.
     *
     * @param annotation an {@link SetConfig} annotation
     */
    public void updateConfiguration(@NotNull final SetConfig annotation) {
        getConfigurationPid(annotation.pid(), annotation.component()).ifPresent(pid -> {
            final Map<String, Object> updated = ComponentPropertyParser.parse(annotation.property());
            updateConfiguration(pid, updated);
        });
    }

    /**
     * Updates a {@link Configuration} from the provided pid and property map.
     *
     * @param pid         the configuration pid
     * @param propertyMap config properties to set on the configuration
     */
    public void updateConfiguration(@NotNull final String pid, @NotNull final Map<String, Object> propertyMap) {
        updatePropertiesForConfigPid(propertyMap, pid, osgiContext.getService(ConfigurationAdmin.class));
    }

    /**
     * Return a concrete instance of the OSGi config / Component Property Type represented by the given
     * {@link ConfigType} annotation discovered via reflection.
     *
     * @param annotation the {@link ConfigType}
     * @return a concrete instance of the type specified by the provided {@link ConfigType#type()}
     */
    public Object constructConfigType(@NotNull final ConfigType annotation) {
        return constructConfigType(annotation, null);
    }

    /**
     * Return a concrete instance of the OSGi config / Component Property Type represented by the given
     * {@link ConfigType} annotation discovered via reflection.
     *
     * @param annotation the {@link ConfigType}
     * @param applyPid   if not empty, override any specified {@link ConfigType#pid()}.
     * @return a concrete instance of the type specified by the provided {@link ConfigType#type()}
     */
    @SuppressWarnings("null")
    public Object constructConfigType(@NotNull final ConfigType annotation, @Nullable final String applyPid) {
        if (!annotation.type().isAnnotation() && !annotation.type().isInterface()) {
            throw new IllegalArgumentException("illegal value for ConfigType type " + annotation.type());
        }
        if (!annotation.lenient()) {
            ComponentPropertyParser.assertOneToOneMapping(annotation.type(), annotation.property());
        }
        final Map<String, Object> merged =
                new HashMap<>(ComponentPropertyParser.parse(annotation.type(), annotation.property()));
        Optional.ofNullable(applyPid)
                .filter(pid -> !pid.isEmpty())
                .or(() -> getConfigurationPid(annotation.pid(), annotation.component()))
                .ifPresent(pid ->
                        mergePropertiesFromConfigPid(merged, pid, osgiContext.getService(ConfigurationAdmin.class)));
        return Annotations.toObject(
                annotation.type(), merged, osgiContext.bundleContext().getBundle(), false);
    }

    /**
     * Construct a collection typed config for the provided annotation.
     *
     * @param annotation a component property type annotation or {@link ConfigType} annotation
     * @return a typed config
     */
    @SuppressWarnings("rawtypes")
    public TypedConfig newTypedConfig(@NotNull final Annotation annotation) {
        return newTypedConfig(annotation, null);
    }

    /**
     * Construct a collection typed config for the provided annotation.
     *
     * @param annotation a component property type annotation or {@link ConfigType} annotation
     * @param applyPid   optional non-empty configuration pid to apply if annotation is a {@link ConfigType}
     * @return a typed config
     */
    @SuppressWarnings({"rawtypes", "null"})
    public TypedConfig newTypedConfig(@NotNull final Annotation annotation, @Nullable final String applyPid) {
        if (annotation instanceof ConfigType) {
            ConfigType osgiConfig = (ConfigType) annotation;
            Class<?> mappingType = osgiConfig.type();
            return AnnotationTypedConfig.newInstance(
                    mappingType, mappingType.cast(constructConfigType(osgiConfig, applyPid)), annotation);
        } else {
            return AnnotationTypedConfig.newInstance(annotation.annotationType(), annotation, annotation);
        }
    }
}
