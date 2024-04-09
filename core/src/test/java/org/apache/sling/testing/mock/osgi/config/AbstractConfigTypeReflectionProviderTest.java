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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class AbstractConfigTypeReflectionProviderTest {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationConfig {
        int size() default 5;

        String name() default "Dave";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface InvalidAnnotationConfig {
        int size() default 5;

        String name() default "Dave";

        AnnotationConfig other_config() default @AnnotationConfig(size = 3, name = "Alice");
    }

    @AnnotationConfig(size = 10, name = "Fred")
    @InvalidAnnotationConfig(size = 10, name = "Fred", other_config = @AnnotationConfig(size = 2, name = "Willy"))
    public static class HasAnnotationConfig {
        @SuppressWarnings("unused")
        private String getInaccessibleValue() {
            return "";
        }

        public String getInvocationTargetValue() {
            throw new NotImplementedException("not implemented!");
        }
    }

    @Test
    public void testAnnotationTypedConfig() throws Exception {
        final AbstractConfigTypeReflectionProvider provider =
                AbstractConfigTypeReflectionProvider.getInstance(AnnotationConfig.class);

        assertTrue(provider.isValidConfigType());

        assertEquals(AnnotationConfig.class, provider.getConfigType());

        assertEquals(
                Map.of("size", 10, "name", "Fred"),
                provider.getPropertyMap(HasAnnotationConfig.class.getAnnotation(AnnotationConfig.class)));

        assertEquals(Map.of("size", 5, "name", "Dave"), provider.getDefaults(Collections.emptyMap()));

        final Object config = new HasAnnotationConfig();
        final Method inaccessibleMethod = HasAnnotationConfig.class.getDeclaredMethod("getInaccessibleValue");
        final Method invocationTargetValue = HasAnnotationConfig.class.getDeclaredMethod("getInvocationTargetValue");

        assertNull(provider.invokeAttribute(inaccessibleMethod, config));
        assertNull(provider.invokeAttribute(invocationTargetValue, config));
    }

    @Test
    public void testInvalidAnnotationTypedConfig() {
        final AbstractConfigTypeReflectionProvider provider =
                AbstractConfigTypeReflectionProvider.getInstance(InvalidAnnotationConfig.class);

        assertFalse(provider.isValidConfigType());

        assertEquals(InvalidAnnotationConfig.class, provider.getConfigType());

        assertEquals(
                Map.of("size", 10, "name", "Fred"),
                provider.getPropertyMap(HasAnnotationConfig.class.getAnnotation(InvalidAnnotationConfig.class)));

        // no defaults are provided when invalid
        assertEquals(Collections.emptyMap(), provider.getDefaults(Collections.emptyMap()));

        assertFalse(provider.addSingleDefault(
                "other.config",
                HasAnnotationConfig.class
                        .getAnnotation(InvalidAnnotationConfig.class)
                        .other_config(),
                Collections.emptyMap()));
    }

    public interface InterfaceConfig {
        int size();

        String name();
    }

    public interface InvalidInterfaceConfig {
        int size();

        String name();

        void setName(String name);
    }

    @Test
    public void testInterfaceTypedConfig() {
        final AbstractConfigTypeReflectionProvider provider =
                AbstractConfigTypeReflectionProvider.getInstance(InterfaceConfig.class);

        assertTrue(provider.isValidConfigType());

        assertEquals(InterfaceConfig.class, provider.getConfigType());

        assertEquals(Collections.emptyMap(), provider.getDefaults(Collections.emptyMap()));

        final InterfaceConfig mocked = mock(InterfaceConfig.class);
        doReturn(10).when(mocked).size();
        assertEquals(Map.of("size", 10), provider.getPropertyMap(mocked));

        final AbstractConfigTypeReflectionProvider invalidProvider =
                AbstractConfigTypeReflectionProvider.getInstance(InvalidInterfaceConfig.class);

        assertFalse(invalidProvider.isValidConfigType());

        assertEquals(InvalidInterfaceConfig.class, invalidProvider.getConfigType());
    }
}
