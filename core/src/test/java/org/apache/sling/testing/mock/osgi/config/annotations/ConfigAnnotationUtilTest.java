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
package org.apache.sling.testing.mock.osgi.config.annotations;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Executable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class ConfigAnnotationUtilTest {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface RuntimeRetained {
        String property() default "default";
    }

    @DynamicConfig(ServiceRanking.class)
    @RuntimeRetained(property = "expected")
    @DynamicConfigs({
            @DynamicConfig(ServiceRanking.class),
            @DynamicConfig(ServiceVendor.class),
    })
    public static class Configured {
        // not used
    }

    @Test
    public void findAnnotations() {
        List<Annotation> annotations = ConfigAnnotationUtil.findAnnotations(Configured.class,
                        Set.of(ServiceRanking.class, ServiceVendor.class, RuntimeRetained.class))
                .collect(Collectors.toList());

        assertEquals(4, annotations.size());

        assertTrue(annotations.get(0) instanceof DynamicConfig);
        assertSame(ServiceRanking.class, ((DynamicConfig) annotations.get(0)).value());

        assertTrue(annotations.get(1) instanceof RuntimeRetained);
        assertEquals("expected", ((RuntimeRetained) annotations.get(1)).property());

        assertTrue(annotations.get(2) instanceof DynamicConfig);
        assertSame(ServiceRanking.class, ((DynamicConfig) annotations.get(2)).value());

        assertTrue(annotations.get(3) instanceof DynamicConfig);
        assertSame(ServiceVendor.class, ((DynamicConfig) annotations.get(3)).value());
    }

    enum AnEnum {
        YES, NO
    }

    static abstract class AnAbstractClass {
        // not used
    }

    interface AnInterface {
        // not used
    }

    @interface AnAnnotation {
        // not used
    }

    @Test
    public void determineSupportedConfigType() {
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(Configured.class).isPresent());
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(AnEnum.class).isPresent());
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(AnAbstractClass.class).isPresent());

        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(Configured[].class).isPresent());
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(AnEnum[].class).isPresent());
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(AnAbstractClass[].class).isPresent());

        assertSame(AnInterface.class, ConfigAnnotationUtil.determineSupportedConfigType(AnInterface.class).orElseThrow());
        assertSame(AnInterface.class, ConfigAnnotationUtil.determineSupportedConfigType(AnInterface[].class).orElseThrow());

        assertSame(AnAnnotation.class, ConfigAnnotationUtil.determineSupportedConfigType(AnAnnotation.class).orElseThrow());
        assertSame(AnAnnotation.class, ConfigAnnotationUtil.determineSupportedConfigType(AnAnnotation[].class).orElseThrow());
    }

    @interface ParameterType1 {
        String value();
    }

    @interface ParameterType2 {
        String value();
    }

    ParameterType1 newMockType1Value(@NotNull final String value) {
        ParameterType1 mocked = mock(ParameterType1.class, withSettings().lenient());
        doReturn(value).when(mocked).value();
        return mocked;
    }

    @Test
    public void resolveParameterToArray() {
        List<ParameterType1> type1Values = List.of(
                newMockType1Value("one"),
                newMockType1Value("two"),
                newMockType1Value("three"));
        ConfigCollection configCollection = mock(ConfigCollection.class);
        doAnswer(call -> type1Values.stream()).when(configCollection).configStream(ParameterType1.class);

        assertArrayEquals(type1Values.toArray(new ParameterType1[0]),
                ConfigAnnotationUtil.resolveParameterToArray(configCollection, ParameterType1.class));
    }

    public static class ExecutableClass {

        /**
         * This accepts 3 single values of type {@link ParameterType1}.
         *
         * @param param0 the first {@link ParameterType1} value
         * @param param1 all the values of type {@link ParameterType1}
         * @param param2 a {@link ParameterType2} value
         * @param param3 the second {@link ParameterType1} value
         * @param param4 the third {@link ParameterType1} value
         */
        public static void execute(ParameterType1 param0,
                                   ParameterType1[] param1,
                                   ParameterType2 param2,
                                   ParameterType1 param3,
                                   ParameterType1 param4) {
            // does nothing
        }

        public static Executable getExecutable() throws NoSuchMethodException {
            return ExecutableClass.class.getMethod("execute",
                    ParameterType1.class,
                    ParameterType1[].class,
                    ParameterType2.class,
                    ParameterType1.class,
                    ParameterType1.class);
        }
    }

    @Test
    public void resolveParameterToValue() throws Exception {
        List<ParameterType1> type1Values = List.of(
                newMockType1Value("one"),
                newMockType1Value("two"),
                newMockType1Value("three"));
        ConfigCollection configCollection = mock(ConfigCollection.class);
        doAnswer(call -> type1Values.stream()).when(configCollection).configStream(ParameterType1.class);

        Executable executable = ExecutableClass.getExecutable();
        assertEquals("one", ConfigAnnotationUtil.resolveParameterToValue(
                configCollection,
                ParameterType1.class,
                executable.getParameterTypes(),
                0).orElseThrow().value());

        // won't resolve an array of the parameter type
        assertNull(ConfigAnnotationUtil.resolveParameterToValue(
                configCollection,
                ParameterType1.class,
                executable.getParameterTypes(),
                1).orElse(null));

        // won't resolve a parameter of a different type
        assertNull(ConfigAnnotationUtil.resolveParameterToValue(
                configCollection,
                ParameterType1.class,
                executable.getParameterTypes(),
                2).orElse(null));

        assertEquals("two", ConfigAnnotationUtil.resolveParameterToValue(
                configCollection,
                ParameterType1.class,
                executable.getParameterTypes(),
                3).orElseThrow().value());

        assertEquals("three", ConfigAnnotationUtil.resolveParameterToValue(
                configCollection,
                ParameterType1.class,
                executable.getParameterTypes(),
                4).orElseThrow().value());
    }
}