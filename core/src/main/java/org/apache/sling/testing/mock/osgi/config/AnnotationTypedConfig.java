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
package org.apache.sling.testing.mock.osgi.config;

import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link TypedConfig}.
 *
 * @param <T> the config type parameter
 */
public final class AnnotationTypedConfig<T> implements TypedConfig<T> {

    private final Class<T> type;
    private final T config;

    private AnnotationTypedConfig(@NotNull Class<T> type, @NotNull T config) {
        this.type = type;
        this.config = config;
    }

    @Override
    @NotNull
    public Class<T> getType() {
        return type;
    }

    @Override
    @NotNull
    public T getConfig() {
        return config;
    }

    /**
     * Constructs a new instance of a {@link AnnotationTypedConfig}.
     *
     * @param type       the config type
     * @param config     the config value
     * @param annotation the annotation that provided the type
     * @param <T>        the config type
     * @return a new instance
     */
    public static <T> AnnotationTypedConfig<T> newInstance(@NotNull Class<T> type,
                                                           @NotNull Object config,
                                                           @NotNull Annotation annotation) {
        if (!type.isInstance(config)) {
            throw new IllegalArgumentException("config " + config + " must be instance of type " +
                    type + " from annotation " + annotation);
        }
        if (annotation instanceof ApplyConfig) {
            if (!((ApplyConfig) annotation).type().isAssignableFrom(type)) {
                throw new IllegalArgumentException("type " + type +
                        " must match config type from annotation " + annotation);
            }
        } else if (!annotation.annotationType().isAssignableFrom(type)) {
            throw new IllegalArgumentException("type " + type + " must match annotation type " + annotation);
        }
        return new AnnotationTypedConfig<>(type, type.cast(config));
    }
}
