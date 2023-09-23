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

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A heterogeneously typed collection of OSGi Config objects for injection as a test parameter.
 */
@ProviderType
public interface ConfigCollection {

    /**
     * Return an untyped stream of all the entries contained in this collection.
     *
     * @return an untyped stream of entries
     */
    Stream<Entry<?>> stream();

    /**
     * Return a typed stream of only those entries in the collection that match the desired config type.
     *
     * @param configType the desired config type
     * @param <T>        the config type
     * @return a typed stream of entries
     */
    default <T> Stream<Entry<T>> stream(@NotNull Class<T> configType) {
        return stream().flatMap(entry -> entry.stream(configType));
    }

    /**
     * Return a typed stream of only those configs in the collection that match the desired config type.
     *
     * @param configType the desired config type
     * @param <T>        the config type
     * @return a typed stream of configs
     */
    default <T> Stream<T> configStream(@NotNull Class<T> configType) {
        return stream().flatMap(entry -> entry.configStream(configType));
    }

    /**
     * Represents a single config entry within the collection. It has its own config type, which may be an
     * type or an interface.
     *
     * @param <T> the config type
     */
    interface Entry<T> {

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
         * Returns a 0- or 1-element entry stream (containing only this entry) depending on whether this entry's
         * {@link #getType()} matches the provided {@code otherType}. This is a convenience method for use in
         * {@link Stream#flatMap(Function)} expressions on the containing {@link ConfigCollection#stream()}.
         *
         * @param otherType the other type to filter by
         * @param <U>       the other type
         * @return a 0- or 1-element entry stream (containing only this entry)
         */
        @SuppressWarnings("unchecked")
        default <U> Stream<Entry<U>> stream(@NotNull Class<U> otherType) {
            if (otherType.equals(getType())) {
                return Stream.of((Entry<U>) this);
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
            return stream(otherType).map(Entry::getConfig);
        }
    }
}
