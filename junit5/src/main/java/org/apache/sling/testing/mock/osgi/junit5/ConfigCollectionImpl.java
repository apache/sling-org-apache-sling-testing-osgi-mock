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
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Encapsulation of lookup operations around a {@link ParameterContext} and {@link ExtensionContext}.
 */
final class ConfigCollectionImpl implements ConfigCollection {

    private final ParameterContext parameterContext;
    private final ExtensionContext extensionContext;
    private final ConfigTypeContext configTypeContext;
    private final String applyPid;

    ConfigCollectionImpl(@NotNull ParameterContext parameterContext,
                         @NotNull ExtensionContext extensionContext,
                         @NotNull ConfigTypeContext configTypeContext,
                         @Nullable String applyPid) {
        this.parameterContext = parameterContext;
        this.extensionContext = extensionContext;
        this.configTypeContext = configTypeContext;
        this.applyPid = applyPid;
    }

    @Override
    public Stream<TypedConfig<?>> stream() {
        return streamConfigTypeAnnotations().map(annotation -> configTypeContext.newTypedConfig(annotation, applyPid));
    }

    Stream<Annotation> streamConfigTypeAnnotations() {
        return Stream.concat(
                extensionContext.getElement().stream()
                        .flatMap(ConfigAnnotationUtil::findConfigTypeAnnotations),
                extensionContext.getParent().stream()
                        .flatMap(parentContext -> ConfigCollectionImpl
                                .collect(parameterContext, parentContext, configTypeContext, applyPid)
                                .streamConfigTypeAnnotations()));
    }

    static ConfigCollectionImpl collect(@NotNull ParameterContext parameterContext,
                                        @NotNull ExtensionContext extensionContext,
                                        @NotNull ConfigTypeContext configTypeContext) {
        return collect(parameterContext, extensionContext, configTypeContext, null);
    }

    static ConfigCollectionImpl collect(@NotNull ParameterContext parameterContext,
                                        @NotNull ExtensionContext extensionContext,
                                        @NotNull ConfigTypeContext configTypeContext,
                                        @Nullable String applyPid) {
        return new ConfigCollectionImpl(parameterContext, extensionContext, configTypeContext, applyPid);
    }
}
