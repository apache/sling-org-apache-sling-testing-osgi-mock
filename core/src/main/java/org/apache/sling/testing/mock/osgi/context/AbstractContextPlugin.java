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

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Default implementation of {@link ContextPlugin}.
 * @param <T> Context
 */
@ConsumerType
public abstract class AbstractContextPlugin<T extends OsgiContextImpl> implements ContextPlugin<T> {

    @Override
    public void beforeSetUp(@NotNull T context) throws Exception {
        // can be overridden by subclasses
    }

    @Override
    public void afterSetUp(@NotNull T context) throws Exception {
        // can be overridden by subclasses
    }

    @Override
    public void beforeTearDown(@NotNull T context) throws Exception {
        // can be overridden by subclasses
    }

    @Override
    public void afterTearDown(@NotNull T context) throws Exception {
        // can be overridden by subclasses
    }
}
