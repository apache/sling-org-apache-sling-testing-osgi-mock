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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.stream.Stream;

final class SingleElementPropertyDefaultsProvider extends AbstractPropertyDefaultsProvider {
    private static final String ATTR_VALUE = "value";
    private final Class<? extends Annotation> annotationType;
    private final String prefix;

    public SingleElementPropertyDefaultsProvider(@NotNull Class<? extends Annotation> annotationType,
                                                 @Nullable String prefix) {
        this.annotationType = annotationType;
        this.prefix = prefix;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    @Override
    public Method[] getMethods() {
        return Stream.of(annotationType.getMethods()).toArray(Method[]::new);
    }

    @Override
    public String getPropertyName(@NotNull final Method method) {
        if (ATTR_VALUE.equals(method.getName())) {
            return ComponentPropertyParser.singleElementAnnotationKey(annotationType().getSimpleName(), prefix);
        } else {
            throw new IllegalArgumentException("only the value method can be mapped to a config property");
        }
    }
}
