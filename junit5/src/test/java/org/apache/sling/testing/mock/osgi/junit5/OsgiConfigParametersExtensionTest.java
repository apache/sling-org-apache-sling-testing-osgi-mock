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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.testing.mock.osgi.config.annotations.AutoConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@AutoConfig(List.class)
@OsgiConfigParametersExtensionTest.ListConfig(size = -5, reverse = false)
@ExtendWith({OsgiConfigParametersExtension.class})
class OsgiConfigParametersExtensionTest {

    @Test
    @ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=1")
    @ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=2")
    @ConfigType(type = ServiceVendor.class, property = "service.vendor=Acme")
    @ConfigType(type = ServiceVendor.class, property = "service.vendor=Blowfly")
    void arrayParams(
            ServiceRanking[] allRankings,
            ServiceRanking serviceRanking1,
            ServiceRanking serviceRanking2,
            ServiceVendor[] allVendors,
            ServiceVendor serviceVendor1,
            ServiceVendor serviceVendor2) {
        assertArrayEquals(
                new Integer[] {serviceRanking1.value(), serviceRanking2.value()},
                Stream.of(allRankings).map(ServiceRanking::value).toArray(Integer[]::new));

        assertArrayEquals(
                new String[] {serviceVendor1.value(), serviceVendor2.value()},
                Stream.of(allVendors).map(ServiceVendor::value).toArray(String[]::new));
    }

    enum AnEnum {
        YES,
        NO
    }

    abstract static class AnAbstractClass {
        // not used
    }

    @Test
    void requireSupportedParameterType() {
        assertThrows(
                ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.requireSupportedParameterType(AnEnum.class));
        assertThrows(
                ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.requireSupportedParameterType(AnAbstractClass.class));
    }

    @Test
    void requireSingleParameterValue() {
        assertThrows(
                ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.requireSingleParameterValue(String.class, null));
        assertThrows(
                ParameterResolutionException.class,
                () -> OsgiConfigParametersExtension.requireSingleParameterValue(String.class, 42));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface PropertyEscaped {
        String prop__name() default "prop__name default";

        String prop_name() default "prop_name default";

        String prop$_$name() default "prop$_$name default";

        String prop$$name() default "prop$$name default";

        String prop$name() default "prop$name default";

        String propName() default "propName default";
    }

    @ConfigType(type = PropertyEscaped.class)
    @ConfigType(
            type = PropertyEscaped.class,
            property = {
                "ignored",
                "prop_name=prop__name value",
                "prop.name=prop_name value",
                "prop-name=prop$_$name value",
                "prop$name=prop$$name value",
                "propname=prop$name value",
                "propName=propName value"
            })
    @Test
    void propertyEscaped(PropertyEscaped defaults, PropertyEscaped withValue) {
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

    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrefixedPropertyEscaped {
        String PREFIX_ = "prefix-"; // this only works if the @interface is also public

        String prop__name() default "prop__name default";

        String prop_name() default "prop_name default";

        String prop$_$name() default "prop$_$name default";

        String prop$$name() default "prop$$name default";

        String prop$name() default "prop$name default";

        String propName() default "propName default";
    }

    @ConfigType(type = PrefixedPropertyEscaped.class, lenient = true)
    @ConfigType(
            type = PrefixedPropertyEscaped.class,
            lenient = true,
            property = {
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
    void prefixedPropertyEscaped(PrefixedPropertyEscaped defaults, PrefixedPropertyEscaped withValue) {
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

    @Retention(RetentionPolicy.RUNTIME)
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

    @ConfigType(type = SingleElementString.class, lenient = true)
    @ConfigType(type = SingleElementString.class, property = "single.element.string=withValue")
    @ConfigType(type = SingleElementStringDefault.class)
    @ConfigType(type = SingleElementStringDefault.class, property = "single.element.string.default=defaultWithValue")
    @ConfigType(type = SingleElementStringArray.class, lenient = true)
    @ConfigType(
            type = SingleElementStringArray.class,
            property = {
                "single.element.string.array=first arrayWithValue",
                "single.element.string.array=second arrayWithValue"
            })
    @ConfigType(type = SingleElementStringArrayDefault.class)
    @ConfigType(
            type = SingleElementStringArrayDefault.class,
            property = {
                "single.element.string.array.default=first arrayDefaultWithValue",
                "single.element.string.array.default=second arrayDefaultWithValue"
            })
    @Test
    void singleElementStrings(
            SingleElementString defaults,
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
        assertArrayEquals(new String[] {"first arrayWithValue", "second arrayWithValue"}, arrayWithValue.value());

        assertArrayEquals(new String[] {"arrayDefaultDefaults"}, arrayDefaultDefaults.value());
        assertArrayEquals(
                new String[] {"first arrayDefaultWithValue", "second arrayDefaultWithValue"},
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

    @ConfigType(type = SingleElementInteger.class, lenient = true)
    @ConfigType(type = SingleElementInteger.class, property = "single.element.integer=1")
    @ConfigType(type = SingleElementIntegerDefault.class)
    @ConfigType(type = SingleElementIntegerDefault.class, property = "single.element.integer.default=2")
    @ConfigType(type = SingleElementIntegerArray.class, lenient = true)
    @ConfigType(
            type = SingleElementIntegerArray.class,
            property = {"single.element.integer.array=10", "single.element.integer.array=11"})
    @ConfigType(type = SingleElementIntegerArrayDefault.class)
    @ConfigType(
            type = SingleElementIntegerArrayDefault.class,
            property = {"single.element.integer.array.default=21", "single.element.integer.array.default=22"})
    @Test
    void singleElementIntegers(
            SingleElementInteger defaults,
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
        assertArrayEquals(new int[] {10, 11}, arrayWithValue.value());

        assertArrayEquals(new int[] {-20}, arrayDefaultDefaults.value());
        assertArrayEquals(new int[] {21, 22}, arrayDefaultWithValue.value());
    }

    public @interface SingleElementClass {
        Class<?> value();
    }

    public @interface SingleElementClassDefault {
        Class<?> value() default String.class;
    }

    public @interface SingleElementClassArray {
        Class<?>[] value();
    }

    public @interface SingleElementClassArrayDefault {
        Class<?>[] value() default {OsgiConfigParametersExtensionTest.class};
    }

    @ConfigType(type = SingleElementClass.class, lenient = true)
    @ConfigType(type = SingleElementClass.class, property = "single.element.class=java.lang.Class")
    @ConfigType(type = SingleElementClassDefault.class)
    @ConfigType(type = SingleElementClassDefault.class, property = "single.element.class.default=java.lang.String")
    @ConfigType(type = SingleElementClassArray.class, lenient = true)
    @ConfigType(
            type = SingleElementClassArray.class,
            property = {"single.element.class.array=java.lang.Integer", "single.element.class.array=java.lang.Float"})
    @ConfigType(type = SingleElementClassArrayDefault.class)
    @ConfigType(
            type = SingleElementClassArrayDefault.class,
            property = {
                "single.element.class.array.default=java.lang.Long",
                "single.element.class.array.default=java.lang.Double"
            })
    @Test
    void singleElementClasses(
            SingleElementClass defaults,
            SingleElementClass withValue,
            SingleElementClassDefault defaultDefaults,
            SingleElementClassDefault defaultWithValue,
            SingleElementClassArray arrayDefaults,
            SingleElementClassArray arrayWithValue,
            SingleElementClassArrayDefault arrayDefaultDefaults,
            SingleElementClassArrayDefault arrayDefaultWithValue) {
        assertNull(defaults.value());
        assertEquals(Class.class, withValue.value());

        assertEquals(String.class, defaultDefaults.value());
        assertEquals(String.class, defaultWithValue.value());

        assertArrayEquals(new Class<?>[0], arrayDefaults.value());
        assertArrayEquals(new Class<?>[] {Integer.class, Float.class}, arrayWithValue.value());

        assertArrayEquals(new Class<?>[] {OsgiConfigParametersExtensionTest.class}, arrayDefaultDefaults.value());
        assertArrayEquals(new Class<?>[] {Long.class, Double.class}, arrayDefaultWithValue.value());
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

    @ConfigType(type = SingleElementEnum.class, lenient = true)
    @ConfigType(type = SingleElementEnum.class, property = "single.element.enum=NO")
    @ConfigType(type = SingleElementEnumDefault.class)
    @ConfigType(type = SingleElementEnumDefault.class, property = "single.element.enum.default=YES")
    @ConfigType(type = SingleElementEnumArray.class, lenient = true)
    @ConfigType(
            type = SingleElementEnumArray.class,
            property = {"single.element.enum.array=YES", "single.element.enum.array=NO"})
    @ConfigType(type = SingleElementEnumArrayDefault.class)
    @ConfigType(
            type = SingleElementEnumArrayDefault.class,
            property = {"single.element.enum.array.default=NO", "single.element.enum.array.default=YES"})
    @Test
    void singleElementEnums(
            SingleElementEnum defaults,
            SingleElementEnum withValue,
            SingleElementEnumDefault defaultDefaults,
            SingleElementEnumDefault defaultWithValue,
            SingleElementEnumArray arrayDefaults,
            SingleElementEnumArray arrayWithValue,
            SingleElementEnumArrayDefault arrayDefaultDefaults,
            SingleElementEnumArrayDefault arrayDefaultWithValue) {
        assertNull(defaults.value());
        assertEquals(YesOrNo.NO, withValue.value());

        assertEquals(YesOrNo.NO, defaultDefaults.value());
        assertEquals(YesOrNo.YES, defaultWithValue.value());

        assertArrayEquals(new YesOrNo[0], arrayDefaults.value());
        assertArrayEquals(new YesOrNo[] {YesOrNo.YES, YesOrNo.NO}, arrayWithValue.value());

        assertArrayEquals(new YesOrNo[] {YesOrNo.YES}, arrayDefaultDefaults.value());
        assertArrayEquals(new YesOrNo[] {YesOrNo.NO, YesOrNo.YES}, arrayDefaultWithValue.value());
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrefixedSingleElementAnnotation {
        String PREFIX_ = "prefix-"; // this only works if the @interface is also public

        String value();
    }

    @ConfigType(type = PrefixedSingleElementAnnotation.class, lenient = true)
    @ConfigType(
            type = PrefixedSingleElementAnnotation.class,
            property = {"prefix-prefixed.single.element.annotation=crazy, right?"})
    @Test
    void prefixedSingleElementAnnotation(
            PrefixedSingleElementAnnotation defaults, PrefixedSingleElementAnnotation withValue) {
        assertNull(defaults.value());
        assertEquals("crazy, right?", withValue.value());
    }

    public @interface PrefixedSingleElementAnnotationWithDefault {
        String PREFIX_ = "prefix-"; // this only works if the @interface is also public

        String value() default "expect me";
    }

    @ConfigType(type = PrefixedSingleElementAnnotationWithDefault.class)
    @ConfigType(
            type = PrefixedSingleElementAnnotationWithDefault.class,
            property = {"prefix-prefixed.single.element.annotation.with.default=crazy, right?"})
    @Test
    void prefixedSingleElementAnnotation(
            PrefixedSingleElementAnnotationWithDefault defaults, PrefixedSingleElementAnnotationWithDefault withValue) {
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

    @ConfigType(type = PrimitiveProperties.class, lenient = true)
    @ConfigType(
            type = PrimitiveProperties.class,
            property = {
                "boolValue=true",
                "byteValue=10",
                "charValue=1",
                "shortValue=10",
                "intValue=100",
                "longValue=1000",
                "floatValue=11.0",
                "doubleValue=111.0"
            })
    @ConfigType(type = PrimitivePropertiesDefaults.class)
    @ConfigType(
            type = PrimitivePropertiesDefaults.class,
            property = {
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
    void primitiveProperties(
            PrimitiveProperties defaults,
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

    public static class ConcreteParameter {}

    public static class ConcreteParameterExtension implements ParameterResolver {
        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return ConcreteParameter.class.isAssignableFrom(
                    parameterContext.getParameter().getType());
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return new ConcreteParameter();
        }
    }

    @ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=1")
    @ExtendWith(ConcreteParameterExtension.class)
    @Test
    void supportedAndUnsupportedParameter(ConcreteParameter unsupported, ServiceRanking serviceRanking) {
        // this should execute and not throw
        assertNotNull(unsupported);
        assertEquals(1, serviceRanking.value());
    }

    @Test
    // overrides the @AutoConfig(List.class) on the class
    @AutoConfig(Object.class)
    // this annotation's value cannot update ConfigurationAdmin in any way other than via @AutoConfig
    @PrefixedSingleElementAnnotation("will it update")
    // this PrefixedSingleElementAnnotation is initially bound to "other-pid", so it won't get picked up by @AutoConfig
    @ConfigType(
            type = PrefixedSingleElementAnnotation.class,
            pid = "other-pid",
            property = "prefix-prefixed.single.element.annotation=not updated")
    void autoConfig(
            PrefixedSingleElementAnnotation retained,
            PrefixedSingleElementAnnotation constructed,
            @CollectConfigTypes(component = Object.class) ConfigCollection configs) {
        assertEquals("will it update", retained.value());
        assertEquals("not updated", constructed.value());
        // the ConfigCollection applies the java.lang.Object configuration to all collected @ConfigTypes
        assertTrue(configs.configStream(PrefixedSingleElementAnnotation.class)
                .allMatch(config -> "will it update".equals(config.value())));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ListConfig {
        int size();

        boolean reverse();
    }

    @Test
    // @SetConfig is evaluated first
    @SetConfig(component = Object.class, property = "size:Integer=42")
    // this config type is bound and injected first, but sets no size of its own
    @ConfigType(type = ListConfig.class, pid = "other-pid", lenient = true)
    // this config type is not bound and does not read config from config admin, and is injected second
    @ListConfig(size = 10, reverse = true)
    void autoConfig1(
            @CollectConfigTypes(component = Object.class) ConfigCollection objectPidConfigs,
            @CollectConfigTypes(component = List.class) ConfigCollection listPidConfigs) {
        assertEquals(42, objectPidConfigs.firstConfig(ListConfig.class).size());
        assertEquals(10, listPidConfigs.firstConfig(ListConfig.class).size());
    }

    @Test
    // @SetConfig is evaluated first
    @SetConfig(component = Object.class, property = "size:Integer=33")
    // this config type is bound and injected first, but sets no size of its own
    @ConfigType(type = ListConfig.class, pid = "other-pid", lenient = true)
    // this config type is not bound and does not read config from config admin, and is injected second
    @ListConfig(size = 15, reverse = true)
    void autoConfig2(
            @CollectConfigTypes(component = Object.class) ConfigCollection objectPidConfigs,
            @CollectConfigTypes(component = List.class) ConfigCollection listPidConfigs) {
        assertEquals(33, objectPidConfigs.firstConfig(ListConfig.class).size());
        assertEquals(15, listPidConfigs.firstConfig(ListConfig.class).size());
    }

    @Test
    @SingleElementString("SingleElementString")
    @PrefixedSingleElementAnnotation("PrefixedSingleElementAnnotation")
    void configMapParameter(
            @ConfigMap(SingleElementString.class) Map<String, Object> configMap,
            @ConfigMap(PrefixedSingleElementAnnotation.class) Map<String, Object> prefixedMap) {
        assertEquals(Map.of("single.element.string", "SingleElementString"), configMap);
        assertEquals(
                Map.of("prefix-prefixed.single.element.annotation", "PrefixedSingleElementAnnotation"), prefixedMap);
    }

    public static final class TestClass {
        @SingleElementString("a value")
        public void testMethod1(@ConfigMap(SingleElementString.class) Map<String, Object> configMap) {}

        @ConfigType(type = SingleElementString.class, lenient = true)
        public void testMethod2(@ConfigMap(ConfigType.class) Map<String, Object> configMap) {}

        public void testMethod3(Map<String, Object> configMap) {}
    }

    public static final class TestContext extends OsgiContextImpl {
        public void setUpContext() {
            super.setUp();
        }

        public void tearDownContext() {
            super.tearDown();
        }
    }

    @Test
    void isConfigMapParameterType() throws Exception {
        Map<String, Boolean> expectations = Map.of(
                "testMethod1", true,
                "testMethod2", false,
                "testMethod3", false);

        final TestClass testInstance = new TestClass();

        final ExtensionContext parentExtensionContext = mock(ExtensionContext.class);
        doReturn(Optional.empty()).when(parentExtensionContext).getParent();
        doReturn(Optional.of(TestClass.class)).when(parentExtensionContext).getElement();
        doReturn(testInstance).when(parentExtensionContext).getRequiredTestInstance();

        final ExtensionContext extensionContext = mock(ExtensionContext.class);
        doReturn(Optional.of(parentExtensionContext)).when(extensionContext).getParent();
        final ExtensionContext.Store store = mock(ExtensionContext.Store.class);
        doReturn(store).when(extensionContext).getStore(OsgiConfigParametersStore.NAMESPACE);

        doReturn(testInstance).when(extensionContext).getRequiredTestInstance();
        final TestContext context = new TestContext();
        context.setUpContext();
        doReturn(context).when(store).get(testInstance, OsgiContextImpl.class);

        final ParameterContext parameterContext = mock(ParameterContext.class);

        OsgiConfigParametersExtension extension = new OsgiConfigParametersExtension();
        for (Map.Entry<String, Boolean> entry : expectations.entrySet()) {
            Method method = TestClass.class.getMethod(entry.getKey(), Map.class);
            doReturn(method).when(parameterContext).getDeclaringExecutable();
            doReturn(method.getParameters()[0]).when(parameterContext).getParameter();
            doReturn(Optional.of(method)).when(extensionContext).getElement();
            doReturn(0).when(parameterContext).getIndex();
            assertEquals(entry.getValue(), extension.isConfigMapParameterType(parameterContext, extensionContext));
        }
    }
}
