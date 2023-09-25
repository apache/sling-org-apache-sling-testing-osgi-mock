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

import org.apache.sling.testing.mock.osgi.config.annotations.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link org.junit.rules.TestRule} that collects runtime-retained component property type annotations and
 * {@link ApplyConfig} annotations from the current test method
 * and test class.
 */
public class ConfigCollector implements TestRule, ConfigCollection {
    private final OsgiContext osgiContext;
    private final Set<Class<?>> configTypes;
    private final String applyPid;
    private Context context = null;

    /**
     * Create a new instance around the provided {@link OsgiContext} and one or more allowed desired config type
     * classes. Specify a non-empty applyPid value to override the {@link ApplyConfig#pid()} attributes of
     * any collected {@link ApplyConfig} annotations.
     *
     * @param osgiContext a osgi context
     * @param configType  one desired config type
     * @param configTypes additional desired config types
     */
    public ConfigCollector(@NotNull final OsgiContext osgiContext,
                           @NotNull final Class<?> configType,
                           @NotNull final Class<?>... configTypes) {
        this(osgiContext, "", configType, configTypes);
    }

    /**
     * Create a new instance around the provided {@link OsgiContext} and one or more allowed desired config type
     * classes. Specify a non-empty applyPid value to override the {@link ApplyConfig#pid()} attributes of
     * any collected {@link ApplyConfig} annotations.
     *
     * @param osgiContext a osgi context
     * @param applyPid    specify a non-empty configuration pid
     * @param configType  one desired config type
     * @param configTypes additional desired config types
     */
    public ConfigCollector(@NotNull final OsgiContext osgiContext,
                           @NotNull final String applyPid,
                           @NotNull final Class<?> configType,
                           @NotNull final Class<?>... configTypes) {
        this.osgiContext = osgiContext;
        this.applyPid = applyPid;
        this.configTypes = Stream.concat(Stream.of(configType), Arrays.stream(configTypes)).collect(Collectors.toSet());
    }

    @Override
    public Stream<TypedConfig<?>> stream() {
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

    private class Context implements ConfigCollection {

        private final List<TypedConfig<?>> entries;

        public Context(@NotNull final Description description) {
            final List<Annotation> annotations = new ArrayList<>(description.getAnnotations());
            annotations.addAll(Arrays.asList(description.getTestClass().getAnnotations()));
            entries = ConfigAnnotationUtil.findAnnotations(annotations, ConfigCollector.this.configTypes)
                    .map(annotation -> osgiContext.newTypedConfig(annotation, applyPid))
                    .collect(Collectors.toList());
        }

        @Override
        public Stream<TypedConfig<?>> stream() {
            return entries.stream();
        }
    }
}
