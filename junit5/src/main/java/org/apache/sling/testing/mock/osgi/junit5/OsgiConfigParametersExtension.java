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
package org.apache.sling.testing.mock.osgi.junit5;

import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides a lightweight alternative to {@link OsgiContext#registerInjectActivateService(Class, Map)} which relies on
 * exported SCR xml files, for constructing configured instances of OSGi service components under test. This is not a
 * replacement for fully testing OCD metatype annotations through the SCR machinery, but it is useful for unit
 * testing when the internal config annotations are accessible to the test class and all the constructor depedencies
 * would be mocked anyway.
 * <p>
 * For this to work for your annotation type, you must specify
 * {@code @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)} on the type, or use
 * {@link ApplyConfig} to declare that your config type is
 * supported as a test parameter, as well as to specify a list of config properties to map to the type's attributes.
 */
public class OsgiConfigParametersExtension implements ParameterResolver {

    /**
     * Gets or creates the {@link OsgiContext} for the provided extension context.
     *
     * @param extensionContext the extension context
     * @return the {@link OsgiContext}
     */
    private OsgiContext getOsgiContext(@NotNull ExtensionContext extensionContext) {
        return OsgiContextStore.getOrCreateOsgiContext(extensionContext, extensionContext.getRequiredTestInstance());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();
        return ConfigCollection.class.isAssignableFrom(parameterType)
                || ConfigAnnotationUtil.determineSupportedConfigType(parameterType)
                .map(paramType -> ConfigCollectionImpl.collect(parameterContext, extensionContext,
                                getOsgiContext(extensionContext),
                                Collections.singleton(paramType))
                        .streamAnnotations().findAny().isPresent())
                .orElse(false);
    }

    static Class<?> requireSupportedParameterType(@NotNull Class<?> type) throws ParameterResolutionException {
        final Class<?> parameterType = ConfigAnnotationUtil.determineSupportedConfigType(type).orElse(null);
        if (parameterType == null) {
            throw new ParameterResolutionException("Not a supported parameter type " + type);
        }
        return parameterType;
    }

    static Set<Class<?>> checkConfigTypes(@Nullable CollectConfigTypes configTypesAnnotation) throws ParameterResolutionException {
        if (configTypesAnnotation == null) {
            throw new ParameterResolutionException("cannot resolve parameter of type " +
                    ConfigCollection.class + " without a " + CollectConfigTypes.class + " annotation.");
        }
        return Arrays.stream(configTypesAnnotation.value()).collect(Collectors.toSet());
    }

    static Object requireSingleParameterValue(@NotNull Class<?> parameterType, @Nullable Object resolvedValue) throws ParameterResolutionException {
        if (!parameterType.isInstance(resolvedValue)) {
            throw new ParameterResolutionException("failed to resolve parameter value of type " + parameterType + " (value " + resolvedValue + ")");
        }
        return resolvedValue;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (ConfigCollection.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            CollectConfigTypes configTypes = parameterContext.findAnnotation(CollectConfigTypes.class)
                    .orElse(null);
            String applyPid = Optional.ofNullable(configTypes).map(CollectConfigTypes::applyPid).orElse("");
            return ConfigCollectionImpl.collect(parameterContext, extensionContext,
                    getOsgiContext(extensionContext),
                    checkConfigTypes(configTypes), applyPid);
        }
        final boolean isArray = parameterContext.getParameter().getType().isArray();
        final Class<?> parameterType = requireSupportedParameterType(parameterContext.getParameter().getType());
        ConfigCollection configCollection = ConfigCollectionImpl.collect(parameterContext, extensionContext,
                getOsgiContext(extensionContext), Collections.singleton(parameterType));
        if (isArray) {
            return ConfigAnnotationUtil.resolveParameterToArray(configCollection, parameterType);
        } else {
            Object value = ConfigAnnotationUtil.resolveParameterToValue(configCollection,
                    parameterType,
                    parameterContext.getDeclaringExecutable().getParameterTypes(),
                    parameterContext.getIndex()).orElse(null);
            return requireSingleParameterValue(parameterType, value);
        }
    }
}
