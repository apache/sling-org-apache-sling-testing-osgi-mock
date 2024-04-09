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
import java.util.stream.Stream;

import org.apache.sling.testing.mock.osgi.config.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.ConfigTypeContext;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Encapsulation of lookup operations around a {@link ParameterContext} and {@link ExtensionContext}.
 */
final class ConfigCollectionImpl implements ConfigCollection {

    private final ExtensionContext extensionContext;
    private final ConfigTypeContext configTypeContext;
    private final ConfigAnnotationUtil.ConfigTypePredicate configTypePredicate;
    private final String applyPid;

    ConfigCollectionImpl(
            @NotNull ExtensionContext extensionContext,
            @NotNull ConfigTypeContext configTypeContext,
            @Nullable ConfigAnnotationUtil.ConfigTypePredicate configTypePredicate,
            @Nullable String applyPid) {
        this.extensionContext = extensionContext;
        this.configTypeContext = configTypeContext;
        this.configTypePredicate = configTypePredicate;
        this.applyPid = applyPid;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Stream<TypedConfig> stream() {
        return streamConfigTypeAnnotations().map(annotation -> configTypeContext.newTypedConfig(annotation, applyPid));
    }

    Stream<Annotation> streamConfigTypeAnnotations() {
        return Stream.concat(
                extensionContext.getElement().stream()
                        .flatMap(element ->
                                ConfigAnnotationUtil.findConfigTypeAnnotations(element, configTypePredicate)),
                extensionContext.getParent().stream().flatMap(parentContext -> ConfigCollectionImpl.collect(
                                parentContext, configTypeContext, configTypePredicate, applyPid)
                        .streamConfigTypeAnnotations()));
    }

    static ConfigCollectionImpl collect(
            @NotNull ExtensionContext extensionContext, @NotNull ConfigTypeContext configTypeContext) {
        return collect(extensionContext, configTypeContext, null, null);
    }

    static ConfigCollectionImpl collect(
            @NotNull ExtensionContext extensionContext,
            @NotNull ConfigTypeContext configTypeContext,
            @Nullable ConfigAnnotationUtil.ConfigTypePredicate configTypePredicate,
            @Nullable String applyPid) {
        return new ConfigCollectionImpl(extensionContext, configTypeContext, configTypePredicate, applyPid);
    }
}
