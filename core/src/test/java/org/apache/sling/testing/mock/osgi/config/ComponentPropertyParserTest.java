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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ComponentPropertyParserTest {

    enum ValueCardinality {
        ABSENT,
        ONE,
        MANY
    }

    @Test
    public void testPutSingleOrMany() {
        Map<List<String>, ValueCardinality> expectations = Map.of(
                List.of(), ValueCardinality.ABSENT,
                List.of("one"), ValueCardinality.ONE,
                List.of("2"), ValueCardinality.ONE,
                List.of("one", "2"), ValueCardinality.MANY);
        for (Map.Entry<List<String>, ValueCardinality> entry : expectations.entrySet()) {
            Map<String, Object> properties = new HashMap<>();
            ComponentPropertyParser.putSingleOrMany(
                    properties, "test", entry.getKey(), Function.identity(), String[]::new);
            switch (entry.getValue()) {
                case ONE:
                    assertEquals(entry.getKey().get(0), properties.get("test"));
                    break;
                case MANY:
                    assertArrayEquals(entry.getKey().toArray(new String[0]), (String[]) properties.get("test"));
                    break;
                case ABSENT:
                    assertNull(properties.get("test"));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnescapeUnsupported() {
        ComponentPropertyParser.unescape("unsupported");
    }

    @Test
    public void testIdentifierToPropertyName() {
        Map<String, String> expectations = Map.of(
                "prop__name", "prop_name",
                "prop_name", "prop.name",
                "prop$_$name", "prop-name",
                "prop$$name", "prop$name",
                "prop$name", "propname",
                "propName", "propName",
                "two_period_name", "two.period.name");

        String[] prefixes = new String[] {null, "", "prefix-"};
        for (String prefix : prefixes) {
            for (Map.Entry<String, String> entry : expectations.entrySet()) {
                final String expected = entry.getValue();
                assertEquals(
                        Optional.ofNullable(prefix)
                                .map(pfx -> pfx.concat(expected))
                                .orElse(expected),
                        ComponentPropertyParser.identifierToPropertyName(entry.getKey(), prefix));
            }
        }
    }

    @interface InnerAnnotation {
        String value();
    }

    @Test
    public void testSingleElementAnnotationKey() {
        Map<String, String> expectations = Map.of(
                ServiceRanking.class.getSimpleName(),
                "service.ranking",
                ServiceVendor.class.getSimpleName(),
                "service.vendor",
                InnerAnnotation.class.getSimpleName(),
                "inner.annotation",
                "$SomehowStartsWithDollar",
                "$somehow.starts.with.dollar",
                "simpler",
                "simpler",
                "endsWith$",
                "");

        String[] prefixes = new String[] {null, "", "prefix-"};
        for (String prefix : prefixes) {
            for (Map.Entry<String, String> entry : expectations.entrySet()) {
                final String expected = entry.getValue();
                assertEquals(
                        Optional.ofNullable(prefix)
                                .map(pfx -> pfx.concat(expected))
                                .orElse(expected),
                        ComponentPropertyParser.singleElementAnnotationKey(entry.getKey(), prefix));
            }
        }
    }

    public Object[] toObjectArray(boolean[] array) {
        Object[] boxes = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            boxes[i] = array[i];
        }
        return boxes;
    }

    public Object[] toObjectArray(byte[] array) {
        Object[] boxes = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            boxes[i] = array[i];
        }
        return boxes;
    }

    public Object[] toObjectArray(char[] array) {
        Object[] boxes = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            boxes[i] = array[i];
        }
        return boxes;
    }

    public Object[] toObjectArray(short[] array) {
        Object[] boxes = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            boxes[i] = array[i];
        }
        return boxes;
    }

    public Object[] toObjectArray(int[] array) {
        return Arrays.stream(array).boxed().toArray(Object[]::new);
    }

    public Object[] toObjectArray(long[] array) {
        return Arrays.stream(array).boxed().toArray(Object[]::new);
    }

    public Object[] toObjectArray(float[] array) {
        Object[] boxes = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            boxes[i] = array[i];
        }
        return boxes;
    }

    public Object[] toObjectArray(double[] array) {
        return Arrays.stream(array).boxed().toArray(Object[]::new);
    }

    public Object[] toObjectArray(Object[] array) {
        return array;
    }

    public Object[] toObjectArray(@NotNull Object array) {
        if (array instanceof boolean[]) {
            return toObjectArray((boolean[]) array);
        } else if (array instanceof byte[]) {
            return toObjectArray((byte[]) array);
        } else if (array instanceof char[]) {
            return toObjectArray((char[]) array);
        } else if (array instanceof short[]) {
            return toObjectArray((short[]) array);
        } else if (array instanceof int[]) {
            return toObjectArray((int[]) array);
        } else if (array instanceof long[]) {
            return toObjectArray((long[]) array);
        } else if (array instanceof float[]) {
            return toObjectArray((float[]) array);
        } else if (array instanceof double[]) {
            return toObjectArray((double[]) array);
        } else if (array instanceof Object[]) {
            return toObjectArray((Object[]) array);
        }
        return new Object[] {array};
    }

    public void assertMapDeepEquals(Map<String, Object> expected, Map<String, Object> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        for (String key : expected.keySet()) {
            Object value = expected.get(key);
            if (value.getClass().isArray()) {
                assertArrayEquals(toObjectArray(value), toObjectArray(actual.get(key)));
            } else {
                assertEquals(value, actual.get(key));
            }
        }
    }

    public void assertGetAnnotationDefaultsExpectations(
            @NotNull Map<Class<? extends Annotation>, Map<String, Object>> expectations) {
        for (Map.Entry<Class<? extends Annotation>, Map<String, Object>> entry : expectations.entrySet()) {
            final Map<String, Object> expected = entry.getValue();
            final Map<String, Object> actual = new HashMap<>();
            ComponentPropertyParser.getDefaults(entry.getKey(), actual);
            assertMapDeepEquals(expected, actual);
            final Map<String, Object> expectedAllSet = expected.keySet().stream()
                    .collect(Collectors.toMap(Function.identity(), (key) -> new String[] {"im set"}));
            final Map<String, Object> actualAllSet = expected.keySet().stream()
                    .collect(Collectors.toMap(Function.identity(), (key) -> new String[] {"im set"}));
            ComponentPropertyParser.getDefaults(entry.getKey(), actualAllSet);
            assertMapDeepEquals(expectedAllSet, actualAllSet);
        }
    }

    public @interface PropertyEscaped {
        String prop__name() default "prop__name default";

        String prop_name() default "prop_name default";

        String prop$_$name() default "prop$_$name default";

        String prop$$name() default "prop$$name default";

        String prop$name() default "prop$name default";

        String propName() default "propName default";
    }

    public @interface PrefixedPropertyEscaped {
        String PREFIX_ = "prefix-"; // this only works if the @interface is also public

        String prop__name() default "prop__name default";

        String prop_name() default "prop_name default";

        String prop$_$name() default "prop$_$name default";

        String prop$$name() default "prop$$name default";

        String prop$name() default "prop$name default";

        String propName() default "propName default";
    }

    @Test
    public void testGetAnnotationDefaults() {
        Map<Class<? extends Annotation>, Map<String, Object>> expectations = Map.of(
                PropertyEscaped.class,
                Map.of(
                        "prop_name", new String[] {"prop__name default"},
                        "prop.name", new String[] {"prop_name default"},
                        "prop-name", new String[] {"prop$_$name default"},
                        "prop$name", new String[] {"prop$$name default"},
                        "propname", new String[] {"prop$name default"},
                        "propName", new String[] {"propName default"}),
                PrefixedPropertyEscaped.class,
                Map.of(
                        "prefix-prop_name", new String[] {"prop__name default"},
                        "prefix-prop.name", new String[] {"prop_name default"},
                        "prefix-prop-name", new String[] {"prop$_$name default"},
                        "prefix-prop$name", new String[] {"prop$$name default"},
                        "prefix-propname", new String[] {"prop$name default"},
                        "prefix-propName", new String[] {"propName default"}));
        assertGetAnnotationDefaultsExpectations(expectations);
    }

    public @interface SingleElementString {
        String value();
    }

    public @interface SingleElementStringDefault {
        String value() default "defaultDefaults";
    }

    public @interface SingleElementStringArray {
        String[] value();
    }

    public @interface SingleElementStringArrayDefault {
        String[] value() default {"arrayDefaultDefaults"};
    }

    @Test
    public void testGetAnnotationDefaultsSingleElementString() {
        Map<Class<? extends Annotation>, Map<String, Object>> expectations = Map.of(
                SingleElementString.class,
                Collections.emptyMap(),
                SingleElementStringDefault.class,
                Map.of("single.element.string.default", new String[] {"defaultDefaults"}),
                SingleElementStringArray.class,
                Collections.emptyMap(),
                SingleElementStringArrayDefault.class,
                Map.of("single.element.string.array.default", new String[] {"arrayDefaultDefaults"}));
        assertGetAnnotationDefaultsExpectations(expectations);
    }

    public @interface SingleElementClass {
        Class<?> value();
    }

    public @interface SingleElementClassDefault {
        Class<?> value() default ComponentPropertyParser.class;
    }

    public @interface SingleElementClassArray {
        Class<?>[] value();
    }

    public @interface SingleElementClassArrayDefault {
        Class<?>[] value() default {ComponentPropertyParserTest.class};
    }

    @Test
    public void testGetAnnotationDefaultsSingleElementClass() {
        Map<Class<? extends Annotation>, Map<String, Object>> expectations = Map.of(
                SingleElementClass.class,
                Collections.emptyMap(),
                SingleElementClassDefault.class,
                Map.of("single.element.class.default", new String[] {ComponentPropertyParser.class.getName()}),
                SingleElementClassArray.class,
                Collections.emptyMap(),
                SingleElementClassArrayDefault.class,
                Map.of("single.element.class.array.default", new String[] {ComponentPropertyParserTest.class.getName()
                }));
        assertGetAnnotationDefaultsExpectations(expectations);
    }

    public @interface SingleElementInteger {
        int value();
    }

    public @interface SingleElementIntegerDefault {
        int value() default -2;
    }

    public @interface SingleElementIntegerArray {
        int[] value();
    }

    public @interface SingleElementIntegerArrayDefault {
        int[] value() default {-20};
    }

    @Test
    public void testGetAnnotationDefaultsSingleElementInteger() {
        Map<Class<? extends Annotation>, Map<String, Object>> expectations = Map.of(
                SingleElementInteger.class,
                Collections.emptyMap(),
                SingleElementIntegerDefault.class,
                Map.of("single.element.integer.default", new int[] {-2}),
                SingleElementIntegerArray.class,
                Collections.emptyMap(),
                SingleElementIntegerArrayDefault.class,
                Map.of("single.element.integer.array.default", new int[] {-20}));
        assertGetAnnotationDefaultsExpectations(expectations);
    }

    @Test
    public void testParsePrimitives() {
        assertMapDeepEquals(
                Map.of(
                        "single.element.string.default",
                        new String[] {"defaultDefaults"},
                        "string.value",
                        new String[] {"a string"},
                        "boolean.value",
                        new Boolean[] {false, true},
                        "byte.value",
                        new Byte[] {(byte) 20},
                        "char.value",
                        new Character[] {'a', 'x'},
                        "short.value",
                        new Short[] {1},
                        "int.value",
                        new Integer[] {10},
                        "long.value",
                        new Long[] {100L},
                        "float.value",
                        new Float[] {11.0f},
                        "double.value",
                        new Double[] {111.0}),
                ComponentPropertyParser.parse(SingleElementStringDefault.class, new String[] {
                    "ignore",
                    "string.value=a string",
                    "boolean.value:String=false",
                    "boolean.value:Boolean=true",
                    "byte.value:Byte=20",
                    "char.value:Character=abc",
                    "char.value=xyz",
                    "short.value:Short=1",
                    "int.value:Integer=10",
                    "long.value:Long=100",
                    "float.value:Float=11.0",
                    "double.value:Double=111.0"
                }));
    }

    public interface AnInterface {
        String PREFIX_ = "prefix-"; // this only works if the interface is also public

        String anotherProperty();

        String value();
    }

    @Test
    public void testParseInterface() {
        Map<String, Object> props = ComponentPropertyParser.parse(
                AnInterface.class, new String[] {"prefix-value=a value", "prefix-anotherProperty=another value"});

        assertEquals(Map.of("prefix-value", "a value", "prefix-anotherProperty", "another value"), props);
    }

    @Test
    public void testIsSupportedPropertyMapValueType() {
        Stream.of(
                        boolean.class,
                        boolean[].class,
                        Boolean.class,
                        Boolean[].class,
                        byte.class,
                        byte[].class,
                        Byte.class,
                        Byte[].class,
                        char.class,
                        char[].class,
                        Character.class,
                        Character[].class,
                        short.class,
                        short[].class,
                        Short.class,
                        Short[].class,
                        int.class,
                        int[].class,
                        Integer.class,
                        Integer[].class,
                        long.class,
                        long[].class,
                        Long.class,
                        Long[].class,
                        float.class,
                        float[].class,
                        Float.class,
                        Float[].class,
                        double.class,
                        double[].class,
                        Double.class,
                        Double[].class)
                .forEach(type -> assertTrue(ComponentPropertyParser.isSupportedPropertyMapValueType(type)));

        Stream.of(Class.class, Class[].class)
                .forEach(type -> assertFalse(ComponentPropertyParser.isSupportedPropertyMapValueType(type)));

        Stream.of(ConfigAnnotationUtilTest.AnEnum.class, ConfigAnnotationUtilTest.AnEnum[].class)
                .forEach(type -> assertFalse(ComponentPropertyParser.isSupportedPropertyMapValueType(type)));

        Stream.of(ConfigAnnotationUtilTest.AnAbstractClass.class, ConfigAnnotationUtilTest.AnAbstractClass.class)
                .forEach(type -> assertFalse(ComponentPropertyParser.isSupportedPropertyMapValueType(type)));
    }

    @Test
    public void testIsSupportedConfigTypeValueType() {
        Stream.of(
                        boolean.class,
                        boolean[].class,
                        Boolean.class,
                        Boolean[].class,
                        byte.class,
                        byte[].class,
                        Byte.class,
                        Byte[].class,
                        char.class,
                        char[].class,
                        Character.class,
                        Character[].class,
                        short.class,
                        short[].class,
                        Short.class,
                        Short[].class,
                        int.class,
                        int[].class,
                        Integer.class,
                        Integer[].class,
                        long.class,
                        long[].class,
                        Long.class,
                        Long[].class,
                        float.class,
                        float[].class,
                        Float.class,
                        Float[].class,
                        double.class,
                        double[].class,
                        Double.class,
                        Double[].class)
                .forEach(type -> assertTrue(ComponentPropertyParser.isSupportedConfigTypeValueType(type)));

        Stream.of(Class.class, Class[].class)
                .forEach(type -> assertTrue(ComponentPropertyParser.isSupportedConfigTypeValueType(type)));

        Stream.of(ConfigAnnotationUtilTest.AnEnum.class, ConfigAnnotationUtilTest.AnEnum[].class)
                .forEach(type -> assertTrue(ComponentPropertyParser.isSupportedConfigTypeValueType(type)));

        Stream.of(ConfigAnnotationUtilTest.AnAbstractClass.class, ConfigAnnotationUtilTest.AnAbstractClass.class)
                .forEach(type -> assertFalse(ComponentPropertyParser.isSupportedConfigTypeValueType(type)));
    }

    public @interface NotSingleElementAnnotation {
        String anotherProperty() default "another one";

        String value() default "expected";
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleElementAnnotationPropertyDefaultsProvider() throws Exception {
        SingleElementAnnotationReflectionProvider defaultsProvider =
                new SingleElementAnnotationReflectionProvider(NotSingleElementAnnotation.class, null);
        defaultsProvider.getPropertyName(NotSingleElementAnnotation.class.getMethod("anotherProperty"));
    }

    public enum YesOrNo {
        YES,
        NO
    }

    public @interface SingleElementEnum {
        YesOrNo value();
    }

    public @interface SingleElementEnumDefault {
        YesOrNo value() default YesOrNo.NO;
    }

    public @interface SingleElementEnumArray {
        YesOrNo[] value();
    }

    public @interface SingleElementEnumArrayDefault {
        YesOrNo[] value() default {YesOrNo.YES};
    }

    @Test
    public void testGetAnnotationDefaultsSingleElementEnum() {
        Map<Class<? extends Annotation>, Map<String, Object>> expectations = Map.of(
                SingleElementEnum.class,
                Collections.emptyMap(),
                SingleElementEnumDefault.class,
                Map.of("single.element.enum.default", new String[] {"NO"}),
                SingleElementEnumArray.class,
                Collections.emptyMap(),
                SingleElementEnumArrayDefault.class,
                Map.of("single.element.enum.array.default", new String[] {"YES"}));
        assertGetAnnotationDefaultsExpectations(expectations);
    }

    public @interface PropertyEscapedNoDefaults {
        String prop__name();

        String prop_name();

        String prop$_$name();

        String prop$$name();

        String prop$name();

        String propName();
    }

    public @interface PrefixedPropertyEscapedNoDefaults {
        String PREFIX_ = "prefix-"; // this only works if the @interface is also public

        String prop__name();

        String prop_name();

        String prop$_$name();

        String prop$$name();

        String prop$name();

        String propName();
    }

    @Test
    public void testAssertOneToOneMapping() {
        Map<Class<?>, String[]> expectations = Map.of(
                SingleElementString.class,
                new String[] {"single.element.string=value"},
                PropertyEscapedNoDefaults.class,
                new String[] {
                    "prop_name=prop__name",
                    "prop.name=prop_name",
                    "prop-name=prop$_$name",
                    "prop$name=prop$$name",
                    "propname=prop$name",
                    "propName=propName"
                },
                PrefixedPropertyEscapedNoDefaults.class,
                new String[] {
                    "prefix-prop_name=prop__name",
                    "prefix-prop.name=prop_name",
                    "prefix-prop-name=prop$_$name",
                    "prefix-prop$name=prop$$name",
                    "prefix-propname=prop$name",
                    "prefix-propName=propName"
                });
        for (Map.Entry<Class<?>, String[]> entry : expectations.entrySet()) {
            ComponentPropertyParser.assertOneToOneMapping(entry.getKey(), entry.getValue());
        }
    }

    @Test(expected = ConfigTypeStrictnessViolation.class)
    public void testAssertOneToOneMappingMissingExpected() {
        ComponentPropertyParser.assertOneToOneMapping(SingleElementString.class, new String[0]);
    }

    @Test(expected = ConfigTypeStrictnessViolation.class)
    public void testAssertOneToOneMappingUnexpectedParsed() {
        ComponentPropertyParser.assertOneToOneMapping(
                SingleElementString.class,
                new String[] {"single.element.string=value", "single.element.integer:Integer=10"});
    }
}
