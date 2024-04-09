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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.sling.testing.mock.osgi.config.annotations.AutoConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigTypes;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfigs;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.quality.Strictness;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class ConfigAnnotationUtilTest {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface RuntimeRetained {
        String property() default "default";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface InvalidCpt {
        RuntimeRetained nestedAnnotation() default @RuntimeRetained(property = "defaultDefaultdEfAuLt");
    }

    @SetConfig(pid = "first")
    @ConfigType(type = ServiceRanking.class)
    @RuntimeRetained(property = "expected")
    @InvalidCpt
    @SetConfigs({@SetConfig(pid = "second"), @SetConfig(pid = "third")})
    @ConfigTypes({
        @ConfigType(type = ServiceRanking.class),
        @ConfigType(type = ServiceVendor.class),
        @ConfigType(type = ConfigType.class),
        @ConfigType(type = ConfigTypes.class)
    })
    public static class Configured {
        // not used
    }

    @Test
    public void findAnnotationsFromAnnotatedElement() {
        List<Annotation> annotations =
                ConfigAnnotationUtil.findConfigTypeAnnotations(Configured.class).collect(Collectors.toList());

        assertEquals(4, annotations.size());

        assertTrue(annotations.get(0) instanceof ConfigType);
        assertSame(ServiceRanking.class, ((ConfigType) annotations.get(0)).type());

        assertTrue(annotations.get(1) instanceof RuntimeRetained);
        assertEquals("expected", ((RuntimeRetained) annotations.get(1)).property());

        assertTrue(annotations.get(2) instanceof ConfigType);
        assertSame(ServiceRanking.class, ((ConfigType) annotations.get(2)).type());

        assertTrue(annotations.get(3) instanceof ConfigType);
        assertSame(ServiceVendor.class, ((ConfigType) annotations.get(3)).type());
    }

    @Test
    public void findAnnotationsFromCollection() {
        List<Annotation> allAnnotations = Arrays.asList(Configured.class.getAnnotations());
        List<Annotation> annotations =
                ConfigAnnotationUtil.findConfigTypeAnnotations(allAnnotations).collect(Collectors.toList());

        assertEquals(4, annotations.size());

        assertTrue(annotations.get(0) instanceof ConfigType);
        assertSame(ServiceRanking.class, ((ConfigType) annotations.get(0)).type());

        assertTrue(annotations.get(1) instanceof RuntimeRetained);
        assertEquals("expected", ((RuntimeRetained) annotations.get(1)).property());

        assertTrue(annotations.get(2) instanceof ConfigType);
        assertSame(ServiceRanking.class, ((ConfigType) annotations.get(2)).type());

        assertTrue(annotations.get(3) instanceof ConfigType);
        assertSame(ServiceVendor.class, ((ConfigType) annotations.get(3)).type());
    }

    @Test
    public void findUpdateConfigsFromAnnotatedElement() {
        List<SetConfig> annotations = ConfigAnnotationUtil.findUpdateConfigAnnotations(Configured.class)
                .collect(Collectors.toList());

        assertEquals(3, annotations.size());

        assertEquals("first", annotations.get(0).pid());
        assertEquals("second", annotations.get(1).pid());
        assertEquals("third", annotations.get(2).pid());
    }

    @Test
    public void findUpdateConfigsFromCollection() {
        List<Annotation> allAnnotations = Arrays.asList(Configured.class.getAnnotations());
        List<SetConfig> annotations =
                ConfigAnnotationUtil.findUpdateConfigAnnotations(allAnnotations).collect(Collectors.toList());

        assertEquals(3, annotations.size());

        assertEquals("first", annotations.get(0).pid());
        assertEquals("second", annotations.get(1).pid());
        assertEquals("third", annotations.get(2).pid());
    }

    public enum AnEnum {
        YES,
        NO
    }

    public abstract static class AnAbstractClass {
        // not used
    }

    public interface AnInterface {
        // not used
    }

    public @interface AnAnnotation {
        // not used
    }

    @Test
    public void determineSupportedConfigType() {
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(Configured.class)
                .isPresent());
        assertFalse(
                ConfigAnnotationUtil.determineSupportedConfigType(AnEnum.class).isPresent());
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(AnAbstractClass.class)
                .isPresent());

        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(Configured[].class)
                .isPresent());
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(AnEnum[].class)
                .isPresent());
        assertFalse(ConfigAnnotationUtil.determineSupportedConfigType(AnAbstractClass[].class)
                .isPresent());

        assertSame(
                AnInterface.class,
                ConfigAnnotationUtil.determineSupportedConfigType(AnInterface.class)
                        .orElseThrow());
        assertSame(
                AnInterface.class,
                ConfigAnnotationUtil.determineSupportedConfigType(AnInterface[].class)
                        .orElseThrow());

        assertSame(
                AnAnnotation.class,
                ConfigAnnotationUtil.determineSupportedConfigType(AnAnnotation.class)
                        .orElseThrow());
        assertSame(
                AnAnnotation.class,
                ConfigAnnotationUtil.determineSupportedConfigType(AnAnnotation[].class)
                        .orElseThrow());
    }

    public @interface ParameterType1 {
        String value();
    }

    public @interface ParameterType2 {
        String value();
    }

    @SuppressWarnings("null")
    ParameterType1 newMockType1Value(@NotNull final String value) {
        ParameterType1 mocked = mock(ParameterType1.class, withSettings().strictness(Strictness.LENIENT));
        doReturn(value).when(mocked).value();
        return mocked;
    }

    @SuppressWarnings({"null", "unchecked"})
    TypedConfig<ParameterType1> newMockTypedConfig1(@NotNull final String value) {
        ParameterType1 mockedConfig = newMockType1Value(value);
        TypedConfig<ParameterType1> mocked =
                mock(TypedConfig.class, withSettings().strictness(Strictness.LENIENT));
        doReturn(mockedConfig).when(mocked).getConfig();
        doAnswer(call -> Map.of("parameter.type1", value)).when(mocked).getConfigMap();
        return mocked;
    }

    @Test
    @SuppressWarnings("null")
    public void resolveParameterToArray() {
        List<ParameterType1> type1Values =
                List.of(newMockType1Value("one"), newMockType1Value("two"), newMockType1Value("three"));
        ConfigCollection configCollection = mock(ConfigCollection.class);
        doAnswer(call -> type1Values.stream()).when(configCollection).configStream(ParameterType1.class);

        assertArrayEquals(
                type1Values.toArray(new ParameterType1[0]),
                ConfigAnnotationUtil.resolveParameterToArray(configCollection, ParameterType1.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolveFirstParameter() {
        List<ParameterType1> type1Values =
                List.of(newMockType1Value("one"), newMockType1Value("two"), newMockType1Value("three"));
        ConfigCollection configCollection = mock(ConfigCollection.class);
        doAnswer(call -> type1Values.stream()).when(configCollection).configStream(ParameterType1.class);
        doCallRealMethod().when(configCollection).firstConfig(any(Class.class));
        assertEquals(type1Values.get(0), configCollection.firstConfig(ParameterType1.class));
    }

    @Test(expected = NoSuchElementException.class)
    @SuppressWarnings("unchecked")
    public void resolveFirstParameterAbsent() {
        List<ParameterType1> type1Values =
                List.of(newMockType1Value("one"), newMockType1Value("two"), newMockType1Value("three"));
        ConfigCollection configCollection = mock(ConfigCollection.class);
        doAnswer(call -> type1Values.stream()).when(configCollection).configStream(ParameterType1.class);
        doCallRealMethod().when(configCollection).firstConfig(any(Class.class));
        configCollection.firstConfig(ParameterType2.class);
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
        public static void execute(
                ParameterType1 param0,
                ParameterType1[] param1,
                ParameterType2 param2,
                ParameterType1 param3,
                ParameterType1 param4) {
            // does nothing
        }

        public static Executable getExecutable() throws NoSuchMethodException {
            return ExecutableClass.class.getMethod(
                    "execute",
                    ParameterType1.class,
                    ParameterType1[].class,
                    ParameterType2.class,
                    ParameterType1.class,
                    ParameterType1.class);
        }
    }

    @Test
    @SuppressWarnings({"null", "unused"})
    public void resolveParameterToValueOrConfigMap() throws Exception {
        List<TypedConfig<ParameterType1>> type1Values =
                List.of(newMockTypedConfig1("one"), newMockTypedConfig1("two"), newMockTypedConfig1("three"));
        ConfigCollection configCollection = mock(ConfigCollection.class);
        doAnswer(call -> type1Values.stream()).when(configCollection).stream(ParameterType1.class);

        Executable executable = ExecutableClass.getExecutable();
        // expect empty result if index is less than 0
        assertNull(ConfigAnnotationUtil.resolveParameterToValue(
                        configCollection, ParameterType1.class, executable.getParameterTypes(), -1)
                .orElse(null));

        assertEquals(
                "one",
                ConfigAnnotationUtil.resolveParameterToValue(
                                configCollection, ParameterType1.class, executable.getParameterTypes(), 0)
                        .orElseThrow()
                        .value());
        assertEquals(
                Map.of("parameter.type1", "one"),
                ConfigAnnotationUtil.resolveParameterToConfigMap(
                                configCollection, ParameterType1.class, executable.getParameterTypes(), 0)
                        .orElseThrow());

        // won't resolve an array of the parameter type
        assertNull(ConfigAnnotationUtil.resolveParameterToValue(
                        configCollection, ParameterType1.class, executable.getParameterTypes(), 1)
                .orElse(null));
        assertNull(ConfigAnnotationUtil.resolveParameterToConfigMap(
                        configCollection, ParameterType1.class, executable.getParameterTypes(), 1)
                .orElse(null));

        // won't resolve a parameter of a different type
        assertNull(ConfigAnnotationUtil.resolveParameterToValue(
                        configCollection, ParameterType1.class, executable.getParameterTypes(), 2)
                .orElse(null));
        assertNull(ConfigAnnotationUtil.resolveParameterToConfigMap(
                        configCollection, ParameterType1.class, executable.getParameterTypes(), 2)
                .orElse(null));

        assertEquals(
                "two",
                ConfigAnnotationUtil.resolveParameterToValue(
                                configCollection, ParameterType1.class, executable.getParameterTypes(), 3)
                        .orElseThrow()
                        .value());
        assertEquals(
                Map.of("parameter.type1", "two"),
                ConfigAnnotationUtil.resolveParameterToConfigMap(
                                configCollection, ParameterType1.class, executable.getParameterTypes(), 3)
                        .orElseThrow());

        assertEquals(
                "three",
                ConfigAnnotationUtil.resolveParameterToValue(
                                configCollection, ParameterType1.class, executable.getParameterTypes(), 4)
                        .orElseThrow()
                        .value());
        assertEquals(
                Map.of("parameter.type1", "three"),
                ConfigAnnotationUtil.resolveParameterToConfigMap(
                                configCollection, ParameterType1.class, executable.getParameterTypes(), 4)
                        .orElseThrow());

        // expect empty result if index is equal to the number of signature args
        assertNull(ConfigAnnotationUtil.resolveParameterToValue(
                        configCollection,
                        ParameterType1.class,
                        executable.getParameterTypes(),
                        executable.getParameterTypes().length)
                .orElse(null));
        assertNull(ConfigAnnotationUtil.resolveParameterToConfigMap(
                        configCollection,
                        ParameterType1.class,
                        executable.getParameterTypes(),
                        executable.getParameterTypes().length)
                .orElse(null));

        // expect empty result if index is greater than the number of signature args
        assertNull(ConfigAnnotationUtil.resolveParameterToValue(
                        configCollection,
                        ParameterType1.class,
                        executable.getParameterTypes(),
                        executable.getParameterTypes().length + 1)
                .orElse(null));
        assertNull(ConfigAnnotationUtil.resolveParameterToConfigMap(
                        configCollection,
                        ParameterType1.class,
                        executable.getParameterTypes(),
                        executable.getParameterTypes().length + 1)
                .orElse(null));
    }

    @Test
    public void testIsValidConfigType() {
        assertFalse(ConfigAnnotationUtil.isValidConfigType(AnAbstractClass.class));
        assertFalse(ConfigAnnotationUtil.isValidConfigType(AnEnum.class));
        assertTrue(ConfigAnnotationUtil.isValidConfigType(AnAnnotation.class));
        assertTrue(ConfigAnnotationUtil.isValidConfigType(AnInterface.class));
    }

    @AutoConfig(Void.class)
    public static class AutoConfigured {}

    @Test
    public void testConfigTypeAnnotationFilter() {
        RuntimeRetained cpt = Configured.class.getAnnotation(RuntimeRetained.class);
        assertTrue(ConfigAnnotationUtil.configTypeAnnotationFilter(
                        (parent, configType) -> parent.isEmpty() && configType.equals(cpt.annotationType()))
                .test(cpt));
        ConfigType cta = Configured.class.getAnnotation(ConfigType.class);
        assertFalse(ConfigAnnotationUtil.configTypeAnnotationFilter((parent, configType) -> parent.isEmpty())
                .test(cta));
        InvalidCpt invalidCpt = Configured.class.getAnnotation(InvalidCpt.class);
        assertFalse(ConfigAnnotationUtil.configTypeAnnotationFilter((parent, configType) -> parent.isEmpty())
                .test(invalidCpt));
        // AutoConfig is excluded from the filter
        AutoConfig aca = AutoConfigured.class.getAnnotation(AutoConfig.class);
        assertFalse(ConfigAnnotationUtil.configTypeAnnotationFilter((parent, configType) -> parent.isEmpty())
                .test(aca));
    }
}
