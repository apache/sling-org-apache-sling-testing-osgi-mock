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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test with {@link OsgiContext} with context plugins.
 */
@ExtendWith(OsgiContextExtension.class)
@SuppressWarnings("null")
class OsgiContextPluginTest {

    private final @NotNull OsgiContextCallback contextBeforeSetup = mock(OsgiContextCallback.class);
    private final @NotNull OsgiContextCallback contextAfterSetup = mock(OsgiContextCallback.class);
    private final @NotNull OsgiContextCallback contextBeforeTeardown = mock(OsgiContextCallback.class);
    private final @NotNull OsgiContextCallback contextAfterTeardown = mock(OsgiContextCallback.class);

    private final @NotNull OsgiContext context = new OsgiContextBuilder()
            .beforeSetUp(contextBeforeSetup)
            .afterSetUp(contextAfterSetup)
            .beforeTearDown(contextBeforeTeardown)
            .afterTearDown(contextAfterTeardown)
            .build();

    @BeforeEach
    public void setUp() throws Exception {
        verify(contextBeforeSetup).execute(context);
    }

    @Test
    public void testRequest() throws Exception {
        verify(contextAfterSetup).execute(context);
    }

    @Test
    public void testResourceResolverFactoryActivatorProps() throws Exception {
        verify(contextAfterSetup).execute(context);
    }

    @AfterEach
    public void tearDown() throws Exception {
        verify(contextBeforeTeardown).execute(context);
    }

}
