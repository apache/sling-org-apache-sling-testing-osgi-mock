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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/**
 * Helper class managing storage of {@link OsgiContext} in extension context
 * store.
 */
final class OsgiContextStore {

    private static final Namespace OSGi_CONTEXT_NAMESPACE = Namespace.create(OsgiContextExtension.class);

    private OsgiContextStore() {
        // static methods only
    }

    /**
     * Get {@link OsgiContext} from extension context store.
     * @param extensionContext Extension context
     * @param testInstance Test instance
     * @return OsgiContext or null
     */
    @SuppressWarnings("null")
    public static OsgiContext getOsgiContext(ExtensionContext extensionContext, Object testInstance) {
        return getStore(extensionContext).get(testInstance, OsgiContext.class);
    }

    /**
     * Get {@link OsgiContext} from extension context store - if it does not
     * exist create a new one and store it.
     * @param extensionContext Extension context
     * @param testInstance Test instance
     * @return OsgiContext (never null)
     */
    public static OsgiContext getOrCreateOsgiContext(ExtensionContext extensionContext, Object testInstance) {
        OsgiContext context = getOsgiContext(extensionContext, testInstance);
        if (context == null) {
            context = createOsgiContext(extensionContext);
            storeOsgiContext(extensionContext, testInstance, context);
        }
        return context;
    }

    /**
     * Removes {@link OsgiContext} from extension context store (if it exists).
     * @param extensionContext Extension context
     * @param testInstance Test instance
     */
    public static void removeOsgiContext(ExtensionContext extensionContext, Object testInstance) {
        getStore(extensionContext).remove(testInstance);
    }

    /**
     * Store {@link OsgiContext} in extension context store.
     * @param extensionContext Extension context
     * @param testInstance Test instance
     * @param osgiContext OSGi context
     */
    public static void storeOsgiContext(ExtensionContext extensionContext, Object testInstance, OsgiContext osgiContext) {
        getStore(extensionContext).put(testInstance, osgiContext);
    }

    private static Store getStore(ExtensionContext context) {
        return context.getStore(OSGi_CONTEXT_NAMESPACE);
    }

    private static OsgiContext createOsgiContext(ExtensionContext extensionContext) {
        OsgiContext osgiContext = new OsgiContext();
        osgiContext.setUpContext();
        return osgiContext;
    }

}
