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
 * A heterogeneously typed collection of OSGi Config objects for injection as a test parameter.
 */
@ProviderType
public interface ConfigCollection {

    /**
     * Return an untyped stream of all the typed configs contained in this collection.
     *
     * @return an untyped stream of typed configs
     */
    @SuppressWarnings("rawtypes")
    Stream<TypedConfig> stream();

    /**
     * Return a typed stream of only those entries in the collection that match the desired config type.
     *
     * @param configType the desired config type
     * @param <T>        the config type
     * @return a typed stream of typed configs
     */
    @SuppressWarnings("unchecked")
    default <T> Stream<TypedConfig<T>> stream(@NotNull Class<T> configType) {
        return stream().flatMap(entry -> entry.stream(configType));
    }

    /**
     * Return a typed stream of only those configs in the collection that match the desired config type.
     *
     * @param configType the desired config type
     * @param <T>        the config type
     * @return a typed stream of config values
     */
    @SuppressWarnings("unchecked")
    default <T> Stream<T> configStream(@NotNull Class<T> configType) {
        return stream().flatMap(entry -> entry.configStream(configType));
    }

    /**
     * Return the first available config of type configType, or throw if none are available.
     *
     * @param configType the desired configType
     * @param <T>        the config type
     * @return the first available config value
     * @throws java.util.NoSuchElementException if no matching config is available
     */
    default <T> T firstConfig(@NotNull Class<T> configType) {
        return configStream(configType).findFirst().orElseThrow();
    }

    /**
     * Return the first available config of type configType as a {@link java.util.Map}, or throw if none are available.
     *
     * @param configType the desired configType
     * @param <T>        the config type
     * @return the first available config value as a map
     * @throws java.util.NoSuchElementException if no matching config is available
     */
    default <T> Map<String, Object> firstConfigMap(@NotNull Class<T> configType) {
        return stream(configType).findFirst().orElseThrow().getConfigMap();
    }
}
