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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Helper class managing storage of linked {@link org.apache.sling.testing.mock.osgi.context.OsgiContextImpl} in
 * extension context store.
 */
final class OsgiConfigParametersStore {
    static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(OsgiConfigParametersExtension.class);

    private OsgiConfigParametersStore() {
        // static methods only
    }

    /**
     * Get {@link OsgiContextImpl} from our own extension context store.
     *
     * @param extensionContext Extension context
     * @param testInstance     Test instance
     * @return OsgiContextImpl or null
     */
    @SuppressWarnings("null")
    public static OsgiContextImpl getOsgiContextImpl(ExtensionContext extensionContext, Object testInstance) {
        return getStore(extensionContext).get(testInstance, OsgiContextImpl.class);
    }

    static OsgiContextImpl findInstanceContext(final @NotNull Object testInstance) {
        return getFieldFromTestInstance(testInstance)
                .map(field -> accessOsgiContextImplField(field, testInstance))
                .orElse(null);
    }

    static OsgiContextImpl accessOsgiContextImplField(@NotNull Field osgiContextField, Object testInstance) {
        try {
            return (OsgiContextImpl) osgiContextField.get(testInstance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("failed to access OsgiContextImpl field", e);
        }
    }

    static Optional<Field> getFieldFromTestInstance(Object testInstance) {
        return Optional.ofNullable(testInstance)
                .map(Object::getClass)
                .flatMap(OsgiConfigParametersStore::getFieldFromTestClass);
    }

    @SuppressWarnings("java:S3011")
    static Optional<Field> getFieldFromTestClass(@NotNull Class<?> instanceClass) {
        Field contextField = Arrays.stream(instanceClass.getDeclaredFields())
                .filter(field -> OsgiContextImpl.class.isAssignableFrom(field.getType()))
                .findFirst()
                .orElse(null);
        if (contextField != null) {
            contextField.setAccessible(true);
            return Optional.of(contextField);
        }
        return Optional.ofNullable(instanceClass.getSuperclass())
                .flatMap(OsgiConfigParametersStore::getFieldFromTestClass);
    }

    /**
     * Get {@link OsgiContextImpl} from extension context store. If it does not exist, look for a populated field on the
     * test instance. If that doesn't exist, create a new one and store it.
     *
     * @param extensionContext Extension context
     * @param testInstance     Test instance
     * @return OsgiContext (never null)
     */
    public static OsgiContextImpl getOrCreateOsgiContext(ExtensionContext extensionContext, Object testInstance) {
        OsgiContextImpl context = Optional.ofNullable(getOsgiContextImpl(extensionContext, testInstance))
                .or(() -> Optional.ofNullable(findInstanceContext(testInstance)))
                .orElseGet(() -> OsgiContextStore.getOrCreateOsgiContext(extensionContext, testInstance));
        storeOsgiContext(extensionContext, testInstance, context);
        return context;
    }

    /**
     * Store {@link OsgiContextImpl} in extension context store.
     *
     * @param extensionContext Extension context
     * @param testInstance     Test instance
     * @param osgiContext      OSGi context
     */
    public static void storeOsgiContext(
            ExtensionContext extensionContext, Object testInstance, OsgiContextImpl osgiContext) {
        getStore(extensionContext).put(testInstance, osgiContext);
    }

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
