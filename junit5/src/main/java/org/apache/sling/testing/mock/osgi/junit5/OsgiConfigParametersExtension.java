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

import org.apache.sling.testing.mock.osgi.config.ConfigTypeContext;
import org.apache.sling.testing.mock.osgi.config.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.UpdateConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@link org.junit.jupiter.api.extension.Extension} which uses JUnit5 context reflection to discover OSGi
 * Config Type Annotations present on test methods and classes, and then injects them as test parameters. For this to
 * work for your config type annotation, you must specify {@code @Retention(RetentionPolicy.RUNTIME)} on the desired
 * config type annotation class, or use {@link ConfigType} to declare that your config type is supported as a test
 * parameter. This extension is also responsible for discovering {@link UpdateConfig} annotations and installing them
 * into the {@link OsgiContextImpl}'s ConfigurationAdmin service.
 */
public class OsgiConfigParametersExtension implements ParameterResolver, BeforeEachCallback {

    /**
     * Gets or creates the {@link ConfigTypeContext} for the provided extension context.
     *
     * @param extensionContext the extension context
     * @return the {@link ConfigTypeContext}
     */
    private ConfigTypeContext getConfigTypeContext(@NotNull ExtensionContext extensionContext) {
        return new ConfigTypeContext(OsgiConfigParametersStore.getOrCreateOsgiContext(extensionContext,
                extensionContext.getRequiredTestInstance()));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();
        return ConfigCollection.class.isAssignableFrom(parameterType)
                || ConfigAnnotationUtil.determineSupportedConfigType(parameterType)
                .map(paramType -> ConfigCollectionImpl.collect(parameterContext, extensionContext,
                                getConfigTypeContext(extensionContext),
                                Collections.singleton(paramType))
                        .streamConfigTypeAnnotations().findAny().isPresent())
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

    static Stream<UpdateConfig> streamUpdateConfigAnnotations(ExtensionContext extensionContext) {
        return Stream.concat(
                extensionContext.getParent().stream()
                        .flatMap(OsgiConfigParametersExtension::streamUpdateConfigAnnotations),
                extensionContext.getElement().stream()
                        .flatMap(ConfigAnnotationUtil::findUpdateConfigAnnotations));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        streamUpdateConfigAnnotations(extensionContext)
                .forEachOrdered(getConfigTypeContext(extensionContext)::updateConfiguration);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (ConfigCollection.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            CollectConfigTypes configTypes = parameterContext.findAnnotation(CollectConfigTypes.class)
                    .orElse(null);
            final ConfigTypeContext configTypeContext = getConfigTypeContext(extensionContext);
            String applyPid = Optional.ofNullable(configTypes)
                    .flatMap(annotation -> configTypeContext.getConfigurationPid(annotation.pid(), annotation.component()))
                    .orElse("");
            return ConfigCollectionImpl.collect(parameterContext, extensionContext,
                    configTypeContext, checkConfigTypes(configTypes), applyPid);
        }
        final boolean isArray = parameterContext.getParameter().getType().isArray();
        final Class<?> parameterType = requireSupportedParameterType(parameterContext.getParameter().getType());
        ConfigCollection configCollection = ConfigCollectionImpl.collect(parameterContext, extensionContext,
                getConfigTypeContext(extensionContext), Collections.singleton(parameterType));
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
