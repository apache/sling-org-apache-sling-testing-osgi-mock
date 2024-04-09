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

import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OsgiConfigParametersStoreTest {

    private OsgiContextImpl context = new OsgiContextImpl();

    @Test
    void accessOsgiContextImplField() throws Exception {
        final Field contextField = getClass().getDeclaredField("context");
        assertThrows(
                IllegalStateException.class,
                () -> OsgiConfigParametersStore.accessOsgiContextImplField(contextField, this));

        final Field accessibleField =
                OsgiConfigParametersStore.getFieldFromTestClass(getClass()).orElseThrow();
        assertSame(context, OsgiConfigParametersStore.accessOsgiContextImplField(accessibleField, this));
    }
}
