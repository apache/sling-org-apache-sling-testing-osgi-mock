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
package org.apache.sling.testing.mock.osgi.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Collects list of context plugins.
 */
@ProviderType
public final class ContextPlugins {

    private final @NotNull List<ContextPlugin<? extends OsgiContextImpl>> plugins = new ArrayList<>();

    /**
     * Start with empty list.
     */
    public ContextPlugins() {
        // empty list
    }

    /**
     * Start with some callbacks.
     * @param <T> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     */
    public <T extends OsgiContextImpl> ContextPlugins(@NotNull final ContextCallback<T> afterSetUpCallback) {
        addAfterSetUpCallback(afterSetUpCallback);
    }

    /**
     * Start with some callbacks.
     * @param <U> context type
     * @param <V> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     */
    public <U extends OsgiContextImpl, V extends OsgiContextImpl> ContextPlugins(
            @NotNull final ContextCallback<U> afterSetUpCallback,
            @NotNull final ContextCallback<V> beforeTearDownCallback) {
        addAfterSetUpCallback(afterSetUpCallback);
        addBeforeTearDownCallback(beforeTearDownCallback);
    }

    /**
     * Add plugin
     * @param plugin Plugin
     */
    @SafeVarargs
    public final void addPlugin(@NotNull ContextPlugin<? extends OsgiContextImpl> @NotNull ... plugin) {
        Stream.of(plugin).filter(Objects::nonNull).forEach(plugins::add);
    }

    /**
     * Add callback
     * @param beforeSetUpCallback Allows the application to register an own callback function that is called before the built-in setup rules are executed.
     */
    @SuppressWarnings("null")
    @SafeVarargs
    public final void addBeforeSetUpCallback(
            @NotNull final ContextCallback<? extends OsgiContextImpl> @NotNull ... beforeSetUpCallback) {
        Stream.of(beforeSetUpCallback).filter(Objects::nonNull).forEach(this::addBeforeSetUpCallbackItem);
    }

    private final <T extends OsgiContextImpl> void addBeforeSetUpCallbackItem(@NotNull final ContextCallback<T> item) {
        plugins.add(new AbstractContextPlugin<T>() {
            @Override
            public void beforeSetUp(@NotNull T context) throws Exception {
                item.execute(context);
            }

            @Override
            public String toString() {
                return item.toString();
            }
        });
    }

    /**
     * Add callback
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     */
    @SuppressWarnings("null")
    @SafeVarargs
    public final void addAfterSetUpCallback(
            @NotNull final ContextCallback<? extends OsgiContextImpl> @NotNull ... afterSetUpCallback) {
        Stream.of(afterSetUpCallback).filter(Objects::nonNull).forEach(this::addAfterSetUpCallbackItem);
    }

    private final <T extends OsgiContextImpl> void addAfterSetUpCallbackItem(@NotNull final ContextCallback<T> item) {
        plugins.add(new AbstractContextPlugin<T>() {
            @Override
            public void afterSetUp(@NotNull T context) throws Exception {
                item.execute(context);
            }

            @Override
            public String toString() {
                return item.toString();
            }
        });
    }

    /**
     * Add callback
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     */
    @SuppressWarnings("null")
    @SafeVarargs
    public final void addBeforeTearDownCallback(
            @NotNull final ContextCallback<? extends OsgiContextImpl> @NotNull ... beforeTearDownCallback) {
        Stream.of(beforeTearDownCallback).filter(Objects::nonNull).forEach(this::addBeforeTearDownCallbackItem);
    }

    private final <T extends OsgiContextImpl> void addBeforeTearDownCallbackItem(
            @NotNull final ContextCallback<T> item) {
        plugins.add(new AbstractContextPlugin<T>() {
            @Override
            public void beforeTearDown(@NotNull T context) throws Exception {
                item.execute(context);
            }

            @Override
            public String toString() {
                return item.toString();
            }
        });
    }

    /**
     * Add callback
     * @param afterTearDownCallback Allows the application to register an own callback function that is after before the built-in teardown rules are executed.
     */
    @SuppressWarnings("null")
    @SafeVarargs
    public final void addAfterTearDownCallback(
            @NotNull final ContextCallback<? extends OsgiContextImpl> @NotNull ... afterTearDownCallback) {
        Stream.of(afterTearDownCallback).filter(Objects::nonNull).forEach(this::addAfterTearDownCallbackItem);
    }

    private final <T extends OsgiContextImpl> void addAfterTearDownCallbackItem(
            @NotNull final ContextCallback<T> item) {
        plugins.add(new AbstractContextPlugin<T>() {
            @Override
            public void afterTearDown(@NotNull T context) throws Exception {
                item.execute(context);
            }

            @Override
            public String toString() {
                return item.toString();
            }
        });
    }

    /**
     * @return All plugins
     */
    public @NotNull Collection<ContextPlugin<? extends OsgiContextImpl>> getPlugins() {
        return plugins;
    }

    /**
     * Execute all before setup callbacks.
     * @param <T> context type
     * @param context Context
     */
    @SuppressWarnings("unchecked")
    public <T extends OsgiContextImpl> void executeBeforeSetUpCallback(@NotNull final T context) {
        for (ContextPlugin plugin : plugins) {
            try {
                plugin.beforeSetUp(context);
            } catch (Throwable ex) {
                throw new RuntimeException("Before setup failed (" + plugin.toString() + "): " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Execute all after setup callbacks.
     * @param <T> context type
     * @param context Context
     */
    @SuppressWarnings("unchecked")
    public <T extends OsgiContextImpl> void executeAfterSetUpCallback(@NotNull final T context) {
        for (ContextPlugin plugin : plugins) {
            try {
                plugin.afterSetUp(context);
            } catch (Throwable ex) {
                throw new RuntimeException("After setup failed (" + plugin.toString() + "): " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Execute all before teardown callbacks.
     * @param <T> context type
     * @param context Context
     */
    @SuppressWarnings("unchecked")
    public <T extends OsgiContextImpl> void executeBeforeTearDownCallback(@NotNull final T context) {
        for (ContextPlugin plugin : plugins) {
            try {
                plugin.beforeTearDown(context);
            } catch (Throwable ex) {
                throw new RuntimeException(
                        "Before teardown failed (" + plugin.toString() + "): " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Execute all after teardown callbacks.
     * @param <T> context type
     * @param context Context
     */
    @SuppressWarnings("unchecked")
    public <T extends OsgiContextImpl> void executeAfterTearDownCallback(@NotNull final T context) {
        for (ContextPlugin plugin : plugins) {
            try {
                plugin.afterTearDown(context);
            } catch (Throwable ex) {
                throw new RuntimeException("After teardown failed (" + plugin.toString() + "): " + ex.getMessage(), ex);
            }
        }
    }
}
