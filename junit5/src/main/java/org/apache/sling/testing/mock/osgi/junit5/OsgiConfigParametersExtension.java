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

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.testing.mock.osgi.config.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.ConfigTypeContext;
import org.apache.sling.testing.mock.osgi.config.annotations.AutoConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * An {@link org.junit.jupiter.api.extension.Extension} which uses JUnit5 context reflection to discover OSGi
 * Config Type Annotations present on test methods and classes, and then injects them as test parameters. For this to
 * work for your config type annotation, you must specify {@code @Retention(RetentionPolicy.RUNTIME)} on the desired
 * config type annotation class, or use {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigType}
 * to declare that your config type is supported as a test parameter. This extension is also responsible for discovering
 * {@link SetConfig} annotations and installing them into the
 * {@link org.apache.sling.testing.mock.osgi.context.OsgiContextImpl}'s ConfigurationAdmin service.
 */
public class OsgiConfigParametersExtension implements ParameterResolver, BeforeEachCallback {
    // JUnit's annotations are noise we can filter out right at the start.
    private static final ConfigAnnotationUtil.ConfigTypePredicate DEFAULT_CONFIG_TYPE_PREDICATE =
            (parent, configType) -> !configType.getPackageName().startsWith("org.junit");

    /**
     * Gets or creates the {@link ConfigTypeContext} for the provided extension context.
     *
     * @param extensionContext the extension context
     * @return the {@link ConfigTypeContext}
     */
    private static ConfigTypeContext getConfigTypeContext(@NotNull ExtensionContext extensionContext) {
        return new ConfigTypeContext(OsgiConfigParametersStore.getOrCreateOsgiContext(
                extensionContext, extensionContext.getRequiredTestInstance()));
    }

    static Class<?> requireSupportedParameterType(@NotNull Class<?> type) throws ParameterResolutionException {
        final Class<?> parameterType =
                ConfigAnnotationUtil.determineSupportedConfigType(type).orElse(null);
        if (parameterType == null) {
            throw new ParameterResolutionException("Not a supported parameter type " + type);
        }
        return parameterType;
    }

    static Object requireSingleParameterValue(@NotNull Class<?> parameterType, @Nullable Object resolvedValue)
            throws ParameterResolutionException {
        if (!parameterType.isInstance(resolvedValue)) {
            throw new ParameterResolutionException(
                    "failed to resolve parameter value of type " + parameterType + " (value " + resolvedValue + ")");
        }
        return resolvedValue;
    }

    static Stream<SetConfig> streamUpdateConfigAnnotations(ExtensionContext extensionContext) {
        return Stream.concat(
                extensionContext.getParent().stream()
                        .flatMap(OsgiConfigParametersExtension::streamUpdateConfigAnnotations),
                extensionContext.getElement().stream().flatMap(ConfigAnnotationUtil::findUpdateConfigAnnotations));
    }

    static Stream<Annotation> streamUnboundTypedConfigAnnotations(
            @NotNull ConfigTypeContext context, @NotNull ExtensionContext extensionContext) {
        return Stream.concat(
                extensionContext.getParent().stream()
                        .flatMap(parentContext -> OsgiConfigParametersExtension.streamUnboundTypedConfigAnnotations(
                                context, parentContext)),
                extensionContext.getElement().stream()
                        .flatMap(element -> ConfigAnnotationUtil.findConfigTypeAnnotations(
                                element, DEFAULT_CONFIG_TYPE_PREDICATE.and((annotation, configType) -> annotation
                                        .map(some ->
                                                // only include explicit config annotations or @ConfigType without pids
                                                context.getConfigurationPid(some.pid(), some.component()))
                                        .isEmpty())::test)));
    }

    Optional<AutoConfig> findAutoConfig(ExtensionContext extensionContext) {
        return extensionContext
                .getElement()
                .map(element -> element.getAnnotation(AutoConfig.class))
                .or(() -> extensionContext.getParent().flatMap(this::findAutoConfig));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        final ConfigTypeContext context = getConfigTypeContext(extensionContext);
        streamUpdateConfigAnnotations(extensionContext).forEachOrdered(context::updateConfiguration);

        findAutoConfig(extensionContext)
                .flatMap(annotation -> getConfigTypeContext(extensionContext)
                        .getConfigurationPid(annotation.pid(), annotation.value()))
                .ifPresent(autoPid -> {
                    final Map<String, Object> accumulator = new HashMap<>();
                    streamUnboundTypedConfigAnnotations(context, extensionContext)
                            .map(annotation ->
                                    context.newTypedConfig(annotation).getConfigMap())
                            .forEachOrdered(accumulator::putAll);
                    context.updateConfiguration(autoPid, accumulator);
                });
    }

    boolean isConfigCollectionParameterType(@NotNull Class<?> parameterType) {
        return ConfigCollection.class.isAssignableFrom(parameterType);
    }

    boolean isConfigMapParameterType(
            @NotNull ParameterContext parameterContext, @NotNull ExtensionContext extensionContext) {
        return Map.class.isAssignableFrom(parameterContext.getParameter().getType())
                && getConfigMapParameterConfigType(parameterContext.getParameter(), extensionContext)
                        .isPresent();
    }

    boolean isSupportedConfigType(@NotNull Class<?> parameterType, @NotNull ExtensionContext extensionContext) {
        return ConfigAnnotationUtil.determineSupportedConfigType(parameterType)
                .map(paramType -> ConfigCollectionImpl.collect(extensionContext, getConfigTypeContext(extensionContext))
                        .streamConfigTypeAnnotations()
                        .anyMatch(ConfigAnnotationUtil.configTypeAnnotationFilter(DEFAULT_CONFIG_TYPE_PREDICATE.and(
                                (ann, configType) -> paramType.equals(configType))::test)))
                .orElse(false);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();
        return isConfigCollectionParameterType(parameterType)
                || isConfigMapParameterType(parameterContext, extensionContext)
                || isSupportedConfigType(parameterType, extensionContext);
    }

    @SuppressWarnings("null")
    Object resolveConfigCollectionParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        CollectConfigTypes configTypes =
                parameterContext.findAnnotation(CollectConfigTypes.class).orElse(null);
        final ConfigTypeContext configTypeContext = getConfigTypeContext(extensionContext);
        String applyPid = Optional.ofNullable(configTypes)
                .flatMap(annotation -> configTypeContext.getConfigurationPid(annotation.pid(), annotation.component()))
                .orElse(null);
        ConfigAnnotationUtil.ConfigTypePredicate configTypePredicate = Optional.ofNullable(configTypes)
                .map(ignored -> (ConfigAnnotationUtil.ConfigTypePredicate)
                        DEFAULT_CONFIG_TYPE_PREDICATE.and((parent, configType) -> parent.isPresent())::test)
                .orElse(DEFAULT_CONFIG_TYPE_PREDICATE);
        return ConfigCollectionImpl.collect(extensionContext, configTypeContext, configTypePredicate, applyPid);
    }

    @SuppressWarnings("rawtypes")
    Optional<Class> getConfigMapParameterConfigType(
            @NotNull Parameter parameter, @NotNull ExtensionContext extensionContext) {
        return Optional.ofNullable(parameter.getAnnotation(ConfigMap.class))
                .map(ConfigMap::value)
                .map(Class.class::cast)
                .filter(ignored -> Map.class.isAssignableFrom(parameter.getType()))
                .filter(ConfigAnnotationUtil::isValidConfigType) // filter out by validity before filtering by in-scope
                .filter(configType -> this.isSupportedConfigType(configType, extensionContext));
    }

    Class<?>[] getEffectiveParameterTypes(@NotNull Executable executable, @NotNull ExtensionContext extensionContext) {
        return Arrays.stream(executable.getParameters())
                .map(parameter -> (Class<?>) getConfigMapParameterConfigType(parameter, extensionContext)
                        .orElse(parameter.getType()))
                .toArray(Class[]::new);
    }

    Object resolveConfigMapParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        final Object value = getConfigMapParameterConfigType(parameterContext.getParameter(), extensionContext)
                .flatMap(parameterConfigType -> {
                    ConfigCollection configCollection =
                            ConfigCollectionImpl.collect(extensionContext, getConfigTypeContext(extensionContext));
                    return ConfigAnnotationUtil.resolveParameterToConfigMap(
                            configCollection,
                            (Class<?>) parameterConfigType,
                            getEffectiveParameterTypes(parameterContext.getDeclaringExecutable(), extensionContext),
                            parameterContext.getIndex());
                })
                .orElse(null);
        return requireSingleParameterValue(Map.class, value);
    }

    @SuppressWarnings("null")
    Object resolveConfigTypeParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        final boolean isArray = parameterContext.getParameter().getType().isArray();
        final Class<?> parameterType =
                requireSupportedParameterType(parameterContext.getParameter().getType());
        ConfigCollection configCollection =
                ConfigCollectionImpl.collect(extensionContext, getConfigTypeContext(extensionContext));
        if (isArray) {
            return ConfigAnnotationUtil.resolveParameterToArray(configCollection, parameterType);
        } else {
            Object value = ConfigAnnotationUtil.resolveParameterToValue(
                            configCollection,
                            parameterType,
                            getEffectiveParameterTypes(parameterContext.getDeclaringExecutable(), extensionContext),
                            parameterContext.getIndex())
                    .orElse(null);
            return requireSingleParameterValue(parameterType, value);
        }
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        // check for ConfigCollection parameter first
        if (ConfigCollection.class.isAssignableFrom(
                parameterContext.getParameter().getType())) {
            return resolveConfigCollectionParameter(parameterContext, extensionContext);
        }
        // explicitly check for Map so we short circuit
        if (Map.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            return resolveConfigMapParameter(parameterContext, extensionContext);
        }
        // otherwise resolve config type or config type array parameter
        return resolveConfigTypeParameter(parameterContext, extensionContext);
    }
}
