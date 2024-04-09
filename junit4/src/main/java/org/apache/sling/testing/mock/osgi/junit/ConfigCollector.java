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
package org.apache.sling.testing.mock.osgi.junit;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.testing.mock.osgi.config.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.ConfigTypeContext;
import org.apache.sling.testing.mock.osgi.config.annotations.AutoConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.service.component.annotations.Component;

/**
 * A {@link org.junit.rules.TestRule} that collects runtime-retained component property type annotations and
 * {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigType} annotations from the current test method and
 * test class. This rule is also responsible for discovering
 * {@link org.apache.sling.testing.mock.osgi.config.annotations.SetConfig} annotations and installing them into the
 * provided {@link OsgiContextImpl}'s ConfigurationAdmin service.
 */
public class ConfigCollector implements TestRule, ConfigCollection {
    // JUnit's annotations are noise we can filter out right at the start.
    private static final ConfigAnnotationUtil.ConfigTypePredicate DEFAULT_CONFIG_TYPE_PREDICATE =
            (parent, configType) -> !configType.getPackageName().startsWith("org.junit");
    private final ConfigTypeContext configTypeContext;
    private final String applyPid;
    private Context context = null;

    /**
     * Create a new instance around the provided {@link OsgiContextImpl}.
     *
     * @param osgiContext an osgi context
     */
    public ConfigCollector(@NotNull final OsgiContextImpl osgiContext) {
        this(osgiContext, null, null);
    }

    /**
     * Create a new instance around the provided {@link OsgiContextImpl}.
     *
     * @param osgiContext an osgi context
     * @param pid         the configuration pid to apply
     */
    public ConfigCollector(@NotNull final OsgiContextImpl osgiContext, @NotNull String pid) {
        this(osgiContext, null, pid);
    }

    /**
     * Create a new instance around the provided {@link OsgiContextImpl}.
     *
     * @param osgiContext an osgi context
     * @param component   the component type to use as a configuration pid to apply
     */
    @SuppressWarnings("rawtypes")
    public ConfigCollector(@NotNull final OsgiContextImpl osgiContext, @NotNull Class component) {
        this(osgiContext, component, null);
    }

    /**
     * Create a new instance around the provided {@link OsgiContextImpl}. Specify a non-null applyPid value to override the
     * {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigType#pid()} attributes of any collected
     * {@link org.apache.sling.testing.mock.osgi.config.annotations.ConfigType} annotations.
     *
     * @param osgiContext an osgi context
     * @param component   an optional component type as configuration pid to apply
     * @param pid         specify a non-empty configuration pid
     */
    @SuppressWarnings({"rawtypes", "null"})
    public ConfigCollector(
            @NotNull final OsgiContextImpl osgiContext, @Nullable final Class component, @Nullable final String pid) {
        this.configTypeContext = new ConfigTypeContext(osgiContext);
        this.applyPid = configTypeContext
                .getConfigurationPid(
                        Optional.ofNullable(pid).orElse(Component.NAME),
                        Optional.ofNullable(component).orElse(Void.class))
                .orElse(null);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Stream<TypedConfig> stream() {
        return Optional.ofNullable(context).stream().flatMap(Context::stream);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                context = new Context(description);
                try {
                    base.evaluate();
                } finally {
                    context = null;
                }
            }
        };
    }

    void processSetConfigAnnotations(@NotNull final Description description) {
        final List<Annotation> updateAnnotations =
                new ArrayList<>(Arrays.asList(description.getTestClass().getAnnotations()));
        updateAnnotations.addAll(description.getAnnotations());
        ConfigAnnotationUtil.findUpdateConfigAnnotations(updateAnnotations)
                .forEachOrdered(configTypeContext::updateConfiguration);
    }

    void processAutoConfigAnnotation(@NotNull final Description description) {
        final String autoPid = Optional.ofNullable(description.getAnnotation(AutoConfig.class))
                .or(() -> Optional.ofNullable(description.getTestClass().getAnnotation(AutoConfig.class)))
                .flatMap(autoConfig -> configTypeContext.getConfigurationPid(autoConfig.pid(), autoConfig.value()))
                .orElse(null);

        if (autoPid != null) {
            final List<Annotation> unboundConfigTypes =
                    new ArrayList<>(Arrays.asList(description.getTestClass().getAnnotations()));
            unboundConfigTypes.addAll(description.getAnnotations());
            final Map<String, Object> accumulator = new HashMap<>();
            ConfigAnnotationUtil.findConfigTypeAnnotations(
                            unboundConfigTypes,
                            DEFAULT_CONFIG_TYPE_PREDICATE.and(
                                    // only include explicit config annotations or @ConfigType without pids
                                    (annotation, configType) -> annotation
                                            .map(some ->
                                                    configTypeContext.getConfigurationPid(some.pid(), some.component()))
                                            .isEmpty())::test)
                    .map(annotation ->
                            configTypeContext.newTypedConfig(annotation).getConfigMap())
                    .forEachOrdered(accumulator::putAll);
            configTypeContext.updateConfiguration(autoPid, accumulator);
        }
    }

    @SuppressWarnings("rawtypes")
    List<TypedConfig> collectTypedConfigs(@NotNull final Description description) {
        final List<Annotation> applyAnnotations = new ArrayList<>(description.getAnnotations());
        applyAnnotations.addAll(Arrays.asList(description.getTestClass().getAnnotations()));
        return ConfigAnnotationUtil.findConfigTypeAnnotations(applyAnnotations, DEFAULT_CONFIG_TYPE_PREDICATE)
                .map(annotation -> configTypeContext.newTypedConfig(annotation, applyPid))
                .collect(Collectors.toList());
    }

    private class Context implements ConfigCollection {

        @SuppressWarnings("rawtypes")
        private final List<TypedConfig> entries;

        public Context(@NotNull final Description description) {
            processSetConfigAnnotations(description);

            processAutoConfigAnnotation(description);

            entries = collectTypedConfigs(description);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Stream<TypedConfig> stream() {
            return entries.stream();
        }
    }
}
