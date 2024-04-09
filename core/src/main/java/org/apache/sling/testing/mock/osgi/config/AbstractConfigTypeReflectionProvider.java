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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.felix.scr.impl.inject.internal.Annotations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.testing.mock.osgi.config.ComponentPropertyParser.isSupportedConfigTypeValueType;

/**
 * Base config type reflection provider class.
 */
@ProviderType
abstract class AbstractConfigTypeReflectionProvider {
    private static final Logger log = LoggerFactory.getLogger(AbstractConfigTypeReflectionProvider.class);

    /**
     * Return the property defaults provider that is appropriate for the given annotation type.
     *
     * @param configType the config type
     * @return a property defaults provider
     */
    @SuppressWarnings("unchecked")
    static AbstractConfigTypeReflectionProvider getInstance(@NotNull final Class<?> configType) {
        final String prefix = Annotations.getPrefix(configType);
        if (configType.isAnnotation()) {
            if (Annotations.isSingleElementAnnotation(configType)) {
                return new SingleElementAnnotationReflectionProvider((Class<? extends Annotation>) configType, prefix);
            } else {
                return new AnnotationReflectionProvider((Class<? extends Annotation>) configType, prefix);
            }
        } else {
            return new InterfaceReflectionProvider(configType, prefix);
        }
    }

    abstract Class<?> getConfigType();

    abstract Method[] getMethods();

    abstract String getPropertyName(@NotNull Method method);

    boolean isValidConfigType() {
        return Arrays.stream(getMethods())
                .allMatch(method ->
                        method.getParameterCount() == 0 && isSupportedConfigTypeValueType(method.getReturnType()));
    }

    boolean addSingleDefault(
            @NotNull String propertyName, @NotNull Object value, @NotNull Map<String, Object> defaults) {
        final Object propertyValue = attributeValueToPropertyValue(value);
        if (propertyValue != null) {
            defaults.put(propertyName, propertyValue);
            return true;
        }
        return false;
    }

    Map<String, Object> getDefaults(@NotNull Map<String, Object> existingValues) {
        final Map<String, Object> defaults = new HashMap<>();
        for (Method method : getMethods()) {
            final Object value = method.getDefaultValue();
            if (value != null) {
                // determine property name to set the default value for
                final String propertyName = getPropertyName(method);
                if (existingValues.containsKey(propertyName)) {
                    // skip this default value if the property is already set
                    continue;
                }

                if (!addSingleDefault(propertyName, value, defaults)) {
                    return Collections.emptyMap();
                }
            }
        }
        return defaults;
    }

    Map<String, Object> getPropertyMap(@NotNull Object config) {
        if (getConfigType().isInstance(config)) {
            Map<String, Object> properties = new HashMap<>();
            for (Method method : getMethods()) {
                Object value = invokeAttribute(method, config);
                if (value != null) {
                    final Object propertyValue = attributeValueToPropertyValue(value);
                    if (propertyValue != null) {
                        properties.put(getPropertyName(method), value);
                    }
                }
            }
            return properties;
        }
        return Collections.emptyMap();
    }

    @Nullable
    Object invokeAttribute(Method method, Object config) {
        try {
            return method.invoke(config, (Object[]) null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error(
                    "Failed to invoke config type " + getConfigType() + " method " + method + " on object " + config,
                    e);
            return null;
        }
    }

    @Nullable
    Object attributeValueToPropertyValue(@NotNull Object value) {
        final Class<?> valueType = value.getClass();
        final Class<?> singleType = valueType.isArray() ? valueType.getComponentType() : valueType;
        if (ComponentPropertyParser.isSupportedPropertyMapValueType(singleType)) {
            return value;
        } else if (singleType.equals(Class.class)) {
            // Class.class is the same as Class<Class<?>>
            // this must be transformed to a String representing the FQDN
            if (valueType.isArray()) {
                return Stream.of((Class<?>[]) value).map(Class::getName).toArray(String[]::new);
            } else {
                return ((Class<?>) value).getName();
            }
        } else if (singleType.isEnum()) {
            if (valueType.isArray()) {
                return Stream.of((Enum<?>[]) value).map(Enum::name).toArray(String[]::new);
            } else {
                return ((Enum<?>) value).name();
            }
        } else {
            // every other type of nested member invalid, return false to indicate to caller that
            // all default values for this annotation should be discarded.
            log.warn("illegal member type {} for annotation type", singleType);
            return null;
        }
    }
}
