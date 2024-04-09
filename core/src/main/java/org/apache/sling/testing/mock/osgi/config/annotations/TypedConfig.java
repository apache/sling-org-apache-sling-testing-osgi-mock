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
package org.apache.sling.testing.mock.osgi.config.annotations;

import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a single config entry within a {@link ConfigCollection}. It has its own config type, which may be a
 * annotation type or an interface.
 *
 * @param <T> the config type
 */
@ProviderType
public interface TypedConfig<T> {

    /**
     * The config type. This will only be an annotation type or an interface type.
     *
     * @return the config type
     */
    @NotNull
    Class<T> getType();

    /**
     * The config object. This is the value that is expected to be passed to an
     * {@link org.osgi.service.component.annotations.Activate}-annotated SCR component constructor.
     *
     * @return the config object
     */
    @NotNull
    T getConfig();

    /**
     * Convert the config instance into a traditional config property map.
     *
     * @return a property map
     */
    @NotNull
    Map<String, Object> getConfigMap();

    /**
     * Returns a 0- or 1-element entry stream (containing only this entry) depending on whether this entry's
     * {@link #getType()} matches the provided {@code otherType}. This is a convenience method for use in
     * {@link java.util.stream.Stream#flatMap(Function)} expressions on the containing {@link ConfigCollection#stream()}.
     *
     * @param otherType the other type to filter by
     * @param <U>       the other type
     * @return a 0- or 1-element entry stream (containing only this entry)
     */
    @SuppressWarnings("unchecked")
    default <U> Stream<TypedConfig<U>> stream(@NotNull Class<U> otherType) {
        if (otherType.equals(getType())) {
            return Stream.of((TypedConfig<U>) this);
        } else {
            return Stream.empty();
        }
    }

    /**
     * Returns a 0- or 1-element config stream (containing only this entry's config) depending on whether this
     * entry's {@link #getType()} matches the provided {@code otherType}. This is a convenience method for use in
     * {@link Stream#flatMap(Function)} expressions on the containing {@link ConfigCollection#stream()}.
     *
     * @param otherType the other type to filter by
     * @param <U>       the other type
     * @return a 0- or 1-element config stream (containing only this entry's config)
     */
    default <U> Stream<U> configStream(@NotNull Class<U> otherType) {
        return stream(otherType).map(TypedConfig::getConfig);
    }
}
