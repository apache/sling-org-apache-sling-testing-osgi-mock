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
package org.apache.sling.testing.mock.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * Builds a mocked {@link ComponentContextX}.
 */
public final class ComponentContextBuilder {

    private BundleContext bundleContext;
    private Dictionary<String, Object> properties;
    private Bundle usingBundle;

    ComponentContextBuilder() {
        // constructor package-scope only
    }

    public @NotNull ComponentContextBuilder bundleContext(@NotNull BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        return this;
    }

    public @NotNull ComponentContextBuilder properties(@Nullable Dictionary<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    public @NotNull ComponentContextBuilder properties(@Nullable Map<String, Object> properties) {
        this.properties = MapUtil.toDictionary(properties);
        return this;
    }

    public @NotNull ComponentContextBuilder properties(@NotNull Object @NotNull ... properties) {
        this.properties = MapUtil.toDictionary(properties);
        return this;
    }

    public @NotNull ComponentContextBuilder usingBundle(@NotNull Bundle usingBundle) {
        this.usingBundle = usingBundle;
        return this;
    }

    public @NotNull ComponentContext build() {
        if (bundleContext == null) {
            bundleContext = MockOsgi.newBundleContext();
        }
        if (properties == null) {
            properties = new Hashtable<>();
        }
        return new MockComponentContext((MockBundleContext)bundleContext, properties, usingBundle);
    }

}
