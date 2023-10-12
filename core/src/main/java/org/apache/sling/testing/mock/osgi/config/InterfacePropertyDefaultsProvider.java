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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

final class InterfacePropertyDefaultsProvider extends AbstractPropertyDefaultsProvider {
    private final Class<?> configType;
    private final String prefix;

    public InterfacePropertyDefaultsProvider(@NotNull Class<?> configType, String prefix) {
        this.configType = configType;
        this.prefix = prefix;
    }

    @Override
    Method[] getMethods() {
        return configType.getMethods();
    }

    @Override
    String getPropertyName(@NotNull Method method) {
        return ComponentPropertyParser.identifierToPropertyName(method.getName(), prefix);
    }

    @Override
    Map<String, Object> getDefaults(@NotNull Map<String, Object> existingValues) {
        // interfaces do not have default attribute values.
        return Collections.emptyMap();
    }
}
