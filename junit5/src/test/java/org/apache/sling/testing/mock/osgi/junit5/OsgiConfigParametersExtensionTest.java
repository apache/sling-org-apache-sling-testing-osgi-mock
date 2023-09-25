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

import org.apache.sling.testing.mock.osgi.config.ComponentPropertyParser;
import org.apache.sling.testing.mock.osgi.config.annotations.DynamicConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.DynamicConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OsgiConfigParametersExtension.class)
class OsgiConfigParametersExtensionTest {

    @Test
    @DynamicConfig(value = ServiceRanking.class, property = "service.ranking:Integer=1")
    @DynamicConfig(value = ServiceRanking.class, property = "service.ranking:Integer=2")
    @DynamicConfig(value = ServiceVendor.class, property = "service.vendor=Acme")
    @DynamicConfig(value = ServiceVendor.class, property = "service.vendor=Blowfly")
    void arrayParams(ServiceRanking[] allRankings,
                     ServiceRanking serviceRanking1,
                     ServiceRanking serviceRanking2,
                     ServiceVendor[] allVendors,
                     ServiceVendor serviceVendor1,
                     ServiceVendor serviceVendor2) {
        assertArrayEquals(new Integer[]{serviceRanking1.value(), serviceRanking2.value()},
                Stream.of(allRankings).map(ServiceRanking::value).toArray(Integer[]::new));

        assertArrayEquals(new String[]{serviceVendor1.value(), serviceVendor2.value()},
                Stream.of(allVendors).map(ServiceVendor::value).toArray(String[]::new));
    }

    enum AnEnum {
        YES, NO
    }

    static abstract class AnAbstractClass {
        // not used
    }

    @Test
    void requireSupportedParameterType() {
        assertThrows(ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.requireSupportedParameterType(AnEnum.class));
        assertThrows(ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.requireSupportedParameterType(AnAbstractClass.class));
    }

    @Test
    void requireSingleParameterValue() {
        assertThrows(ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.requireSingleParameterValue(String.class, null));
        assertThrows(ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.requireSingleParameterValue(String.class, 42));
    }

    @Test
    void checkConfigTypes() {
        assertThrows(ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.checkConfigTypes(null));
    }


    public @interface PropertyEscaped {
        String prop__name() default "prop__name default";

        String prop_name() default "prop_name default";

        String prop$_$name() default "prop$_$name default";

        String prop$$name() default "prop$$name default";

        String prop$name() default "prop$name default";

        String propName() default "propName default";
    }

    @DynamicConfig(PropertyEscaped.class)
    @DynamicConfig(value = PropertyEscaped.class, property = {
            "ignored",
            "prop_name=prop__name value",
            "prop.name=prop_name value",
            "prop-name=prop$_$name value",
            "prop$name=prop$$name value",
            "propname=prop$name value",
            "propName=propName value"
    })
    @Test
    void propertyEscaped(PropertyEscaped defaults,
                         PropertyEscaped withValue) {
        assertEquals("prop__name default", defaults.prop__name());
        assertEquals("prop__name value", withValue.prop__name());

        assertEquals("prop_name default", defaults.prop_name());
        assertEquals("prop_name value", withValue.prop_name());

        assertEquals("prop$_$name default", defaults.prop$_$name());
        assertEquals("prop$_$name value", withValue.prop$_$name());

        assertEquals("prop$$name default", defaults.prop$$name());
        assertEquals("prop$$name value", withValue.prop$$name());

        assertEquals("prop$name default", defaults.prop$name());
        assertEquals("prop$name value", withValue.prop$name());

        assertEquals("propName default", defaults.propName());
        assertEquals("propName value", withValue.propName());
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

    @DynamicConfig(PrefixedPropertyEscaped.class)
    @DynamicConfig(value = PrefixedPropertyEscaped.class, property = {
            "prefix-prop_name=prop__name value",
            "prefix-prop.name=prop_name value",
            "prefix-prop-name=prop$_$name value",
            "prefix-prop$name=prop$$name value",
            "prefix-propname=prop$name value",
            "prefix-propName=propName value",
            "prop_name=bzzt. wrong.",
            "prop.name=bzzt. wrong.",
            "prop-name=bzzt. wrong.",
            "prop$name=bzzt. wrong.",
            "propname=bzzt. wrong.",
            "propName=bzzt. wrong."
    })
    @Test
    void prefixedPropertyEscaped(PrefixedPropertyEscaped defaults,
                                 PrefixedPropertyEscaped withValue) {
        assertEquals("prop__name default", defaults.prop__name());
        assertEquals("prop__name value", withValue.prop__name());

        assertEquals("prop_name default", defaults.prop_name());
        assertEquals("prop_name value", withValue.prop_name());

        assertEquals("prop$_$name default", defaults.prop$_$name());
        assertEquals("prop$_$name value", withValue.prop$_$name());

        assertEquals("prop$$name default", defaults.prop$$name());
        assertEquals("prop$$name value", withValue.prop$$name());

        assertEquals("prop$name default", defaults.prop$name());
        assertEquals("prop$name value", withValue.prop$name());

        assertEquals("propName default", defaults.propName());
        assertEquals("propName value", withValue.propName());
    }

    public @interface NestedAnnotationWithDefaults {
        String abcValue() default "abcValue";

        // not allowed for component property types
        DynamicConfigs annotationValue() default @DynamicConfigs;

        String xyzValue() default "xyzValue";
    }

    @DynamicConfig(NestedAnnotationWithDefaults.class)
    @Test
    void nestedAnnotation(NestedAnnotationWithDefaults defaults) {
        // defaults should not be set for types with nested annotations
        assertNull(defaults.abcValue());
        assertNull(defaults.xyzValue());

        // accessing a nested annotation type should throw
        assertThrows(ComponentException.class, defaults::annotationValue);
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

    @DynamicConfig(SingleElementString.class)
    @DynamicConfig(value = SingleElementString.class,
            property = "single.element.string=withValue")
    @DynamicConfig(SingleElementStringDefault.class)
    @DynamicConfig(value = SingleElementStringDefault.class,
            property = "single.element.string.default=defaultWithValue")
    @DynamicConfig(SingleElementStringArray.class)
    @DynamicConfig(value = SingleElementStringArray.class,
            property = {
                    "single.element.string.array=first arrayWithValue",
                    "single.element.string.array=second arrayWithValue"
            })
    @DynamicConfig(SingleElementStringArrayDefault.class)
    @DynamicConfig(value = SingleElementStringArrayDefault.class,
            property = {
                    "single.element.string.array.default=first arrayDefaultWithValue",
                    "single.element.string.array.default=second arrayDefaultWithValue"
            })
    @Test
    void singleElementStrings(SingleElementString defaults,
                              SingleElementString withValue,
                              SingleElementStringDefault defaultDefaults,
                              SingleElementStringDefault defaultWithValue,
                              SingleElementStringArray arrayDefaults,
                              SingleElementStringArray arrayWithValue,
                              SingleElementStringArrayDefault arrayDefaultDefaults,
                              SingleElementStringArrayDefault arrayDefaultWithValue) {
        assertNull(defaults.value());
        assertEquals("withValue", withValue.value());

        assertEquals("defaultDefaults", defaultDefaults.value());
        assertEquals("defaultWithValue", defaultWithValue.value());

        assertArrayEquals(new String[0], arrayDefaults.value());
        assertArrayEquals(new String[]{"first arrayWithValue", "second arrayWithValue"},
                arrayWithValue.value());

        assertArrayEquals(new String[]{"arrayDefaultDefaults"}, arrayDefaultDefaults.value());
        assertArrayEquals(new String[]{"first arrayDefaultWithValue", "second arrayDefaultWithValue"},
                arrayDefaultWithValue.value());
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

    @DynamicConfig(SingleElementInteger.class)
    @DynamicConfig(value = SingleElementInteger.class,
            property = "single.element.integer=1")
    @DynamicConfig(SingleElementIntegerDefault.class)
    @DynamicConfig(value = SingleElementIntegerDefault.class,
            property = "single.element.integer.default=2")
    @DynamicConfig(SingleElementIntegerArray.class)
    @DynamicConfig(value = SingleElementIntegerArray.class,
            property = {
                    "single.element.integer.array=10",
                    "single.element.integer.array=11"
            })
    @DynamicConfig(SingleElementIntegerArrayDefault.class)
    @DynamicConfig(value = SingleElementIntegerArrayDefault.class,
            property = {
                    "single.element.integer.array.default=21",
                    "single.element.integer.array.default=22"
            })
    @Test
    void singleElementIntegers(SingleElementInteger defaults,
                               SingleElementInteger withValue,
                               SingleElementIntegerDefault defaultDefaults,
                               SingleElementIntegerDefault defaultWithValue,
                               SingleElementIntegerArray arrayDefaults,
                               SingleElementIntegerArray arrayWithValue,
                               SingleElementIntegerArrayDefault arrayDefaultDefaults,
                               SingleElementIntegerArrayDefault arrayDefaultWithValue) {
        assertEquals(0, defaults.value());
        assertEquals(1, withValue.value());

        assertEquals(-2, defaultDefaults.value());
        assertEquals(2, defaultWithValue.value());

        assertArrayEquals(new int[0], arrayDefaults.value());
        assertArrayEquals(new int[]{10, 11}, arrayWithValue.value());

        assertArrayEquals(new int[]{-20}, arrayDefaultDefaults.value());
        assertArrayEquals(new int[]{21, 22},
                arrayDefaultWithValue.value());
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
        Class<?>[] value() default {OsgiConfigParametersExtensionTest.class};
    }

    @DynamicConfig(SingleElementClass.class)
    @DynamicConfig(value = SingleElementClass.class,
            property = "single.element.class=java.lang.Class")
    @DynamicConfig(SingleElementClassDefault.class)
    @DynamicConfig(value = SingleElementClassDefault.class,
            property = "single.element.class.default=java.lang.String")
    @DynamicConfig(SingleElementClassArray.class)
    @DynamicConfig(value = SingleElementClassArray.class,
            property = {
                    "single.element.class.array=java.lang.Integer",
                    "single.element.class.array=java.lang.Float"
            })
    @DynamicConfig(SingleElementClassArrayDefault.class)
    @DynamicConfig(value = SingleElementClassArrayDefault.class,
            property = {
                    "single.element.class.array.default=java.lang.Long",
                    "single.element.class.array.default=java.lang.Double"
            })
    @Test
    void singleElementClasses(SingleElementClass defaults,
                              SingleElementClass withValue,
                              SingleElementClassDefault defaultDefaults,
                              SingleElementClassDefault defaultWithValue,
                              SingleElementClassArray arrayDefaults,
                              SingleElementClassArray arrayWithValue,
                              SingleElementClassArrayDefault arrayDefaultDefaults,
                              SingleElementClassArrayDefault arrayDefaultWithValue) {
        assertNull(defaults.value());
        assertEquals(Class.class, withValue.value());

        assertEquals(ComponentPropertyParser.class, defaultDefaults.value());
        assertEquals(String.class, defaultWithValue.value());

        assertArrayEquals(new Class<?>[0], arrayDefaults.value());
        assertArrayEquals(new Class<?>[]{Integer.class, Float.class}, arrayWithValue.value());

        assertArrayEquals(new Class<?>[]{OsgiConfigParametersExtensionTest.class},
                arrayDefaultDefaults.value());
        assertArrayEquals(new Class<?>[]{Long.class, Double.class},
                arrayDefaultWithValue.value());
    }

    public @interface PrefixedSingleElementAnnotation {
        String PREFIX_ = "prefix-"; // this only works if the @interface is also public

        String value();
    }

    @DynamicConfig(PrefixedSingleElementAnnotation.class)
    @DynamicConfig(value = PrefixedSingleElementAnnotation.class, property = {
            "prefix-prefixed.single.element.annotation=crazy, right?"
    })
    @Test
    void prefixedSingleElementAnnotation(PrefixedSingleElementAnnotation defaults,
                                         PrefixedSingleElementAnnotation withValue) {
        assertNull(defaults.value());
        assertEquals("crazy, right?", withValue.value());
    }

    public @interface PrefixedSingleElementAnnotationWithDefault {
        String PREFIX_ = "prefix-"; // this only works if the @interface is also public

        String value() default "expect me";
    }

    @DynamicConfig(PrefixedSingleElementAnnotationWithDefault.class)
    @DynamicConfig(value = PrefixedSingleElementAnnotationWithDefault.class, property = {
            "prefix-prefixed.single.element.annotation.with.default=crazy, right?"
    })
    @Test
    void prefixedSingleElementAnnotation(PrefixedSingleElementAnnotationWithDefault defaults,
                                         PrefixedSingleElementAnnotationWithDefault withValue) {
        assertEquals("expect me", defaults.value());
        assertEquals("crazy, right?", withValue.value());
    }

    @interface PrimitiveProperties {
        boolean boolValue();

        byte byteValue();

        char charValue();

        short shortValue();

        int intValue();

        long longValue();

        float floatValue();

        double doubleValue();
    }

    @interface PrimitivePropertiesDefaults {
        boolean boolValue() default true;

        byte byteValue() default (byte) 42;

        char charValue() default 'z';

        short shortValue() default -10;

        int intValue() default -100;

        long longValue() default -1000L;

        float floatValue() default Float.MIN_VALUE;

        double doubleValue() default Double.MIN_VALUE;
    }

    @DynamicConfig(PrimitiveProperties.class)
    @DynamicConfig(value = PrimitiveProperties.class, property = {
            "boolValue=true",
            "byteValue=10",
            "charValue=1",
            "shortValue=10",
            "intValue=100",
            "longValue=1000",
            "floatValue=11.0",
            "doubleValue=111.0"
    })
    @DynamicConfig(PrimitivePropertiesDefaults.class)
    @DynamicConfig(value = PrimitivePropertiesDefaults.class, property = {
            "boolValue:Boolean=false",
            "byteValue:Byte=20",
            "charValue:Character=2",
            "shortValue:Short=20",
            "intValue:Integer=200",
            "longValue:Long=2000",
            "floatValue:Float=22.0",
            "doubleValue:Double=222.0"
    })
    @Test
    @SuppressWarnings("java:S5961")
    void primitiveProperties(PrimitiveProperties defaults,
                             PrimitiveProperties withValue,
                             PrimitivePropertiesDefaults defaultDefaults,
                             PrimitivePropertiesDefaults defaultWithValue) {
        assertFalse(defaults.boolValue());
        assertTrue(withValue.boolValue());
        assertEquals((byte) 0, defaults.byteValue());
        assertEquals((byte) 10, withValue.byteValue());
        assertEquals((char) 0, defaults.charValue());
        assertEquals('1', withValue.charValue());
        assertEquals((short) 0, defaults.shortValue());
        assertEquals((short) 10, withValue.shortValue());
        assertEquals(0, defaults.intValue());
        assertEquals(100, withValue.intValue());
        assertEquals(0L, defaults.longValue());
        assertEquals(1000L, withValue.longValue());
        assertEquals((float) 0, defaults.floatValue());
        assertEquals(11.0, withValue.floatValue());
        assertEquals(0, defaults.doubleValue());
        assertEquals(111.0D, withValue.doubleValue());

        assertTrue(defaultDefaults.boolValue());
        assertFalse(defaultWithValue.boolValue());
        assertEquals((byte) 42, defaultDefaults.byteValue());
        assertEquals((byte) 20, defaultWithValue.byteValue());
        assertEquals('z', defaultDefaults.charValue());
        assertEquals('2', defaultWithValue.charValue());
        assertEquals((short) -10, defaultDefaults.shortValue());
        assertEquals((short) 20, defaultWithValue.shortValue());
        assertEquals(-100, defaultDefaults.intValue());
        assertEquals(200, defaultWithValue.intValue());
        assertEquals(-1000L, defaultDefaults.longValue());
        assertEquals(2000L, defaultWithValue.longValue());
        assertEquals(Float.MIN_VALUE, defaultDefaults.floatValue());
        assertEquals(22.0, defaultWithValue.floatValue());
        assertEquals(Double.MIN_VALUE, defaultDefaults.doubleValue());
        assertEquals(222.0D, defaultWithValue.doubleValue());
    }

    public static class ConcreteParameter {

    }

    public static class ConcreteParameterExtension implements ParameterResolver {
        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
            return ConcreteParameter.class.isAssignableFrom(parameterContext.getParameter().getType());
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
            return new ConcreteParameter();
        }
    }

    @DynamicConfig(value = ServiceRanking.class, property = "service.ranking:Integer=1")
    @ExtendWith(ConcreteParameterExtension.class)
    @Test
    void supportedAndUnsupportedParameter(ConcreteParameter unsupported, ServiceRanking serviceRanking) {
        // this should execute and not throw
        assertNotNull(unsupported);
        assertEquals(1, serviceRanking.value());
    }
}