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


import org.apache.sling.testing.mock.osgi.config.annotations.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.DynamicConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Encapsulation of lookup operations around a {@link ParameterContext} and {@link ExtensionContext}.
 */
final class ConfigCollectionImpl implements ConfigCollection {

    private final ParameterContext parameterContext;
    private final ExtensionContext extensionContext;
    private final OsgiContext osgiContext;
    private final Set<Class<?>> configTypes;

    ConfigCollectionImpl(@NotNull ParameterContext parameterContext,
                         @NotNull ExtensionContext extensionContext,
                         @NotNull OsgiContext osgiContext,
                         @NotNull Set<Class<?>> configTypes) {
        this.parameterContext = parameterContext;
        this.extensionContext = extensionContext;
        this.osgiContext = osgiContext;
        this.configTypes = Set.copyOf(configTypes);
    }

    @Override
    public Stream<TypedConfig<?>> stream() {
        return streamAnnotations().map(osgiContext::newTypedConfig);
    }

    Stream<Annotation> streamAnnotations() {
        return Stream.concat(
                extensionContext.getElement().stream()
                        .flatMap(element -> ConfigAnnotationUtil.findAnnotations(element, configTypes)),
                extensionContext.getParent().stream()
                        .flatMap(parentContext -> ConfigCollectionImpl
                                .collect(parameterContext, parentContext, osgiContext, configTypes)
                                .streamAnnotations()));
    }

    static ConfigCollectionImpl collect(@NotNull ParameterContext parameterContext,
                                        @NotNull ExtensionContext extensionContext,
                                        @NotNull OsgiContext osgiContext,
                                        @NotNull Set<Class<?>> configTypes) {
        return new ConfigCollectionImpl(parameterContext, extensionContext, osgiContext, configTypes);
    }
}
