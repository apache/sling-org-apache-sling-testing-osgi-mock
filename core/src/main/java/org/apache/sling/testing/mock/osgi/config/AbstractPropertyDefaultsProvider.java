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

import org.apache.felix.scr.impl.inject.Annotations;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@ProviderType
abstract class AbstractPropertyDefaultsProvider {
    private static final Logger log = LoggerFactory.getLogger(AbstractPropertyDefaultsProvider.class);

    /**
     * Return the property defaults provider that is appropriate for the given annotation type.
     *
     * @param annotationType the annotation type
     * @return a proeprty defaults provider
     */
    static AbstractPropertyDefaultsProvider getInstance(@NotNull final Class<? extends Annotation> annotationType) {
        final String prefix = Annotations.getPrefix(annotationType);
        if (Annotations.isSingleElementAnnotation(annotationType)) {
            return new SingleElementPropertyDefaultsProvider(annotationType, prefix);
        } else {
            return new AttributePropertyDefaultsProvider(annotationType, prefix);
        }
    }

    abstract Class<? extends Annotation> annotationType();

    abstract Method[] getMethods();

    abstract String getPropertyName(@NotNull Method method);

    boolean addSingleDefault(@NotNull String propertyName,
                             @NotNull Object value,
                             @NotNull Map<String, Object> defaults) {
        final Class<?> valueType = value.getClass();
        final Class<?> singleType = valueType.isArray() ? valueType.getComponentType() : valueType;

        if (ComponentPropertyParser.isSupportedPropertyMapValueType(singleType)) {
            if (valueType.isArray()) {
                defaults.put(propertyName, value);
            } else {
                Object array = Array.newInstance(singleType, 1);
                Array.set(array, 0, value);
                defaults.put(propertyName, array);
            }
        } else if (singleType.equals(Class.class)) {
            // Class.class is the same as Class<Class<?>>
            // this must be transformed to a String representing the FQDN
            if (valueType.isArray()) {
                defaults.put(propertyName,
                        Stream.of((Class<?>[]) value).map(Class::getName).toArray(String[]::new));
            } else {
                defaults.put(propertyName, new String[]{((Class<?>) value).getName()});
            }
        } else {
            // every other type of nested member invalid, return false to indicate to caller that all default values
            // for this annotation should be discarded.
            log.warn("illegal member type " + singleType + " for annotation type " + annotationType());
            return false;
        }
        return true;
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
}
