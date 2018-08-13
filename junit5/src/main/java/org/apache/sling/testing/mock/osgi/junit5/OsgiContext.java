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

import org.apache.sling.testing.mock.osgi.context.ContextPlugins;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * OSGi Mock parameter object.
 * <p>
 * Additionally you can subclass this class and provide further parameters via
 * {@link OsgiContextBuilder}.
 * </p>
 */
@ConsumerType
public final class OsgiContext extends OsgiContextImpl {

    private final ContextPlugins plugins;
    private boolean isSetUp;

    /**
     * Initialize OSGi context.
     */
    public OsgiContext() {
        this(new ContextPlugins());
    }

    /**
     * Initialize OSGi context.
     * @param contextPlugins Context plugins
     */
    OsgiContext(@NotNull final ContextPlugins contextPlugins) {
        this.plugins = contextPlugins;
    }

    /**
     * This is called by {@link OsgiContextExtension} to set up context.
     */
    protected void setUpContext() {
        isSetUp = true;
        plugins.executeBeforeSetUpCallback(this);
        super.setUp();
    }

    /**
     * This is called by {@link OsgiContextExtension} to tear down context.
     */
    protected void tearDownContext() {
        super.tearDown();
    }

    ContextPlugins getContextPlugins() {
        return plugins;
    }

    boolean isSetUp() {
        return this.isSetUp;
    }

}
