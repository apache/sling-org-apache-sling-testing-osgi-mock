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

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.SetConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null")
public class ConfigTypeContextImplTest {

    static class TestOsgiContext extends OsgiContextImpl {

        void setUpContext() {
            this.setUp();
        }

        void tearDownContext() {
            this.tearDown();
        }
    }

    private TestOsgiContext context;
    private ConfigTypeContext configTypeContext;

    @Before
    public void setUp() throws Exception {
        this.context = new TestOsgiContext();
        this.context.setUpContext();
        this.configTypeContext = new ConfigTypeContext(this.context);
    }

    @After
    public void tearDown() throws Exception {
        this.context.tearDownContext();
    }

    @ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=42")
    public static class Configured {}

    @Test
    public void testConstructConfigType() {
        ConfigType configAnnotation = Configured.class.getAnnotation(ConfigType.class);
        Object reified = configTypeContext.constructConfigType(configAnnotation);
        assertTrue(reified instanceof ServiceRanking);
        ServiceRanking serviceRanking = (ServiceRanking) reified;
        assertEquals(42, serviceRanking.value());
    }

    @ConfigType(type = String.class, property = "service.ranking:Integer=42")
    public static class IllegallyConfigured {}

    @Test(expected = IllegalArgumentException.class)
    public void testConstructConfigTypeThrows() {
        ConfigType configAnnotation = IllegallyConfigured.class.getAnnotation(ConfigType.class);
        configTypeContext.constructConfigType(configAnnotation);
    }

    public @interface ConfigurableFromPid {
        String string_property() default "a default value";
    }

    @ConfigType(
            type = ConfigurableFromPid.class,
            pid = "existing-pid",
            property = {"string.property=a Component.property() value"})
    public static class ConfiguredWithPid {}

    @Test
    public void testConstructConfigTypeWithApplyPid() throws Exception {
        final ConfigurationAdmin configurationAdmin = context.getService(ConfigurationAdmin.class);
        assertNotNull(configurationAdmin);
        final Configuration config = configurationAdmin.getConfiguration("existing-pid");
        final ConfigType configAnnotation = ConfiguredWithPid.class.getAnnotation(ConfigType.class);
        final ConfigType rankingAnnotation = Configured.class.getAnnotation(ConfigType.class);
        assertEquals(
                "a Component.property() value",
                ((ConfigurableFromPid) configTypeContext.constructConfigType(configAnnotation)).string_property());
        assertEquals(42, ((ServiceRanking) configTypeContext.constructConfigType(rankingAnnotation)).value());
        assertEquals(
                "a Component.property() value",
                ((ConfigurableFromPid) configTypeContext.constructConfigType(configAnnotation, "existing-pid"))
                        .string_property());
        assertEquals(
                42,
                ((ServiceRanking) configTypeContext.constructConfigType(rankingAnnotation, "existing-pid")).value());

        config.update(MapUtil.toDictionary(Map.of("string.property", "a configured value", "service.ranking", 99)));

        assertEquals(
                "a configured value",
                ((ConfigurableFromPid) configTypeContext.constructConfigType(configAnnotation)).string_property());
        assertEquals(42, ((ServiceRanking) configTypeContext.constructConfigType(rankingAnnotation)).value());
        assertEquals(
                "a configured value",
                ((ConfigurableFromPid) configTypeContext.constructConfigType(configAnnotation, "existing-pid"))
                        .string_property());
        assertEquals(
                99,
                ((ServiceRanking) configTypeContext.constructConfigType(rankingAnnotation, "existing-pid")).value());

        assertEquals(
                "a Component.property() value",
                ((ConfigurableFromPid) configTypeContext.constructConfigType(configAnnotation, "new-pid"))
                        .string_property());
        assertEquals(
                42, ((ServiceRanking) configTypeContext.constructConfigType(rankingAnnotation, "new-pid")).value());
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdatePropertiesForConfigPidThrows() throws IOException {
        ConfigurationAdmin mocked = mock(ConfigurationAdmin.class);
        doThrow(IOException.class).when(mocked).getConfiguration(anyString());
        Map<String, Object> props = new HashMap<>();
        ConfigTypeContext.updatePropertiesForConfigPid(props, "new-pid", mocked);
    }

    @Test
    public void testUpdatePropertiesForNewConfiguration() throws IOException {
        ConfigurationAdmin mocked = mock(ConfigurationAdmin.class);
        Configuration mockedConfig = mock(Configuration.class);
        doReturn(mockedConfig).when(mocked).getConfiguration("new-pid");
        Map<String, Object> props = Map.of("new.property", "value");
        final Dictionary<String, Object> asDict = MapUtil.toDictionary(props);
        ConfigTypeContext.updatePropertiesForConfigPid(props, "new-pid", mocked);
        verify(mockedConfig, times(1)).update(asDict);
    }

    @Test
    public void testUpdatePropertiesForNullConfigurationAdmin() {
        Map<String, Object> props = new HashMap<>();
        // no throws
        ConfigTypeContext.updatePropertiesForConfigPid(props, "new-pid", null);
        assertTrue(props.isEmpty());
    }

    @SetConfig(pid = "a pid", property = "service.ranking:Integer=42")
    public static class UpdatesConfiguration {}

    @Test
    public void testUpdateConfiguration() throws IOException {
        final ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNull(configAdmin.getConfiguration("a pid").getProperties());
        SetConfig setConfig = UpdatesConfiguration.class.getAnnotation(SetConfig.class);
        configTypeContext.updateConfiguration(setConfig);
        assertEquals(42, configAdmin.getConfiguration("a pid").getProperties().get("service.ranking"));
    }

    @SetConfig(
            pid = "a pid",
            property = {"service.ranking:Integer=42", "service.ranking:Integer=55"})
    public static class UpdatesConfigurationArray {}

    @Test
    public void testUpdateConfigurationArray() throws IOException {
        final ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNull(configAdmin.getConfiguration("a pid").getProperties());
        SetConfig setConfig = UpdatesConfigurationArray.class.getAnnotation(SetConfig.class);
        configTypeContext.updateConfiguration(setConfig);
        assertArrayEquals(new Integer[] {42, 55}, (Integer[])
                configAdmin.getConfiguration("a pid").getProperties().get("service.ranking"));
    }

    @SetConfig(pid = "", property = "service.ranking:Integer=42")
    public static class NoUpdatesConfiguration {}

    @Test
    public void testUpdateConfigurationEmptyPid() throws IOException {
        final ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNull(configAdmin.getConfiguration("").getProperties());
        SetConfig setConfig = NoUpdatesConfiguration.class.getAnnotation(SetConfig.class);
        configTypeContext.updateConfiguration(setConfig);
        assertNull(configAdmin.getConfiguration("").getProperties());
    }

    @SetConfig(component = UpdatesConfigurationByComponentName.class, property = "service.ranking:Integer=42")
    public static class UpdatesConfigurationByComponentName {}

    @Test
    public void testUpdateConfigurationByComponentName() throws IOException {
        final String pid = UpdatesConfigurationByComponentName.class.getName();
        final ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNull(configAdmin.getConfiguration(pid).getProperties());
        SetConfig setConfig = UpdatesConfigurationByComponentName.class.getAnnotation(SetConfig.class);
        configTypeContext.updateConfiguration(setConfig);
        assertEquals(42, configAdmin.getConfiguration(pid).getProperties().get("service.ranking"));
    }

    @SetConfig(pid = "ranking.is.21", property = "service.ranking:Integer=21")
    @SetConfig(pid = "ranking.is.42", property = "service.ranking:Integer=42")
    @SetConfig(component = AppliesMultipleConfigs.class, property = "service.ranking:Integer=55")
    @ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=10")
    public static class AppliesMultipleConfigs {}

    @Test
    public void testConstructConfigTypeMultiplePids() {
        ConfigAnnotationUtil.findUpdateConfigAnnotations(AppliesMultipleConfigs.class)
                .forEachOrdered(configTypeContext::updateConfiguration);
        ConfigType annotation = AppliesMultipleConfigs.class.getAnnotation(ConfigType.class);
        assertEquals(10, ((ServiceRanking) configTypeContext.constructConfigType(annotation)).value());
        assertEquals(10, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "")).value());
        assertEquals(21, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "ranking.is.21")).value());
        assertEquals(42, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "ranking.is.42")).value());
        assertEquals(10, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "new-pid")).value());
        assertEquals(
                55,
                ((ServiceRanking) configTypeContext.constructConfigType(
                                annotation, AppliesMultipleConfigs.class.getName()))
                        .value());
    }

    @SetConfig(pid = "ranking.is.21", property = "service.ranking:Integer=21")
    @SetConfig(pid = "ranking.is.42", property = "service.ranking:Integer=42")
    @SetConfig(component = AppliesMultipleConfigsWithOwnDefault.class, property = "service.ranking:Integer=55")
    @ConfigType(type = ServiceRanking.class, pid = "ranking.is.21", property = "service.ranking:Integer=10")
    public static class AppliesMultipleConfigsWithOwnDefault {}

    @Test
    public void testConstructConfigTypeMultiplePidsWithOwnDefault() {
        ConfigAnnotationUtil.findUpdateConfigAnnotations(AppliesMultipleConfigsWithOwnDefault.class)
                .forEachOrdered(configTypeContext::updateConfiguration);
        ConfigType annotation = AppliesMultipleConfigsWithOwnDefault.class.getAnnotation(ConfigType.class);
        assertEquals(21, ((ServiceRanking) configTypeContext.constructConfigType(annotation)).value());
        assertEquals(21, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "")).value());
        assertEquals(42, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "ranking.is.42")).value());
        assertEquals(10, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "new-pid")).value());
        assertEquals(
                55,
                ((ServiceRanking) configTypeContext.constructConfigType(
                                annotation, AppliesMultipleConfigsWithOwnDefault.class.getName()))
                        .value());
    }

    @SetConfig(pid = "ranking.is.21", property = "service.ranking:Integer=21")
    @SetConfig(pid = "ranking.is.42", property = "service.ranking:Integer=42")
    @SetConfig(component = AppliesMultipleConfigsWithOwnDefaultComponent.class, property = "service.ranking:Integer=55")
    @ConfigType(
            type = ServiceRanking.class,
            pid = Component.NAME,
            component = AppliesMultipleConfigsWithOwnDefaultComponent.class,
            property = "service.ranking:Integer=10")
    public static class AppliesMultipleConfigsWithOwnDefaultComponent {}

    @Test
    public void testConstructConfigTypeMultiplePidsWithOwnDefaultComponent() {
        ConfigAnnotationUtil.findUpdateConfigAnnotations(AppliesMultipleConfigsWithOwnDefaultComponent.class)
                .forEachOrdered(configTypeContext::updateConfiguration);
        ConfigType annotation = AppliesMultipleConfigsWithOwnDefaultComponent.class.getAnnotation(ConfigType.class);
        assertEquals(55, ((ServiceRanking) configTypeContext.constructConfigType(annotation)).value());
        assertEquals(55, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "")).value());
        assertEquals(21, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "ranking.is.21")).value());
        assertEquals(42, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "ranking.is.42")).value());
        assertEquals(10, ((ServiceRanking) configTypeContext.constructConfigType(annotation, "new-pid")).value());
        assertEquals(
                55,
                ((ServiceRanking) configTypeContext.constructConfigType(
                                annotation, AppliesMultipleConfigsWithOwnDefaultComponent.class.getName()))
                        .value());
    }

    @Test(expected = IllegalStateException.class)
    public void testMergePropertiesFromConfigurationPidThrows() throws IOException {
        ConfigurationAdmin mocked = mock(ConfigurationAdmin.class);
        doThrow(IOException.class).when(mocked).getConfiguration(anyString());
        Map<String, Object> props = new HashMap<>();
        ConfigTypeContext.mergePropertiesFromConfigPid(props, "new-pid", mocked);
    }

    @Test
    public void testMergePropertiesFromNewConfiguration() throws IOException {
        ConfigurationAdmin mocked = mock(ConfigurationAdmin.class);
        Configuration mockedConfig = mock(Configuration.class);
        doReturn(mockedConfig).when(mocked).getConfiguration("new-pid");
        Map<String, Object> props = new HashMap<>();
        ConfigTypeContext.mergePropertiesFromConfigPid(props, "new-pid", mocked);
        assertTrue(props.isEmpty());
    }

    @Test
    public void testMergePropertiesFromNullConfigurationAdmin() throws IOException {
        Map<String, Object> props = new HashMap<>();
        ConfigTypeContext.mergePropertiesFromConfigPid(props, "new-pid", null);
        assertTrue(props.isEmpty());
    }

    @Test
    public void testMergePropertiesFromExistingConfiguration() throws IOException {
        Configuration mockedConfig = mock(Configuration.class);
        Map<String, Object> existingProps = Map.of("prop.string", "a string", "prop.integer", 42);
        doAnswer(call -> MapUtil.toDictionary(Map.copyOf(existingProps)))
                .when(mockedConfig)
                .getProperties();

        ConfigurationAdmin mocked = mock(ConfigurationAdmin.class);
        doReturn(mockedConfig).when(mocked).getConfiguration("existing-pid");

        Map<String, Object> props = new HashMap<>();
        ConfigTypeContext.mergePropertiesFromConfigPid(props, "existing-pid", mocked);
        assertEquals(Map.of("prop.string", "a string", "prop.integer", 42), props);

        Map<String, Object> overrideProps =
                new HashMap<>(Map.of("prop.string", "a different string", "prop.boolean", true));

        ConfigTypeContext.mergePropertiesFromConfigPid(overrideProps, "existing-pid", mocked);
        assertEquals(Map.of("prop.string", "a string", "prop.boolean", true, "prop.integer", 42), overrideProps);
    }

    public interface AnInterface {
        int service_ranking();
    }

    @ConfigType(type = AnInterface.class, property = "service.ranking:Integer=42")
    public static class ConfiguredWithInterface {}

    @Test
    public void testConfigTypeInterface() {
        ConfigType configAnnotation = ConfiguredWithInterface.class.getAnnotation(ConfigType.class);
        Object reified = configTypeContext.constructConfigType(configAnnotation);
        assertTrue(reified instanceof AnInterface);
        AnInterface serviceRanking = (AnInterface) reified;
        assertEquals(42, serviceRanking.service_ranking());
    }

    @Test
    public void testNewTypedConfig() {
        ConfigType configAnnotation = Configured.class.getAnnotation(ConfigType.class);
        TypedConfig<?> typedConfig = configTypeContext.newTypedConfig(configAnnotation);
        assertTrue(typedConfig.getConfig() instanceof ServiceRanking);
        ServiceRanking serviceRanking = (ServiceRanking) typedConfig.getConfig();
        assertEquals(42, serviceRanking.value());
        TypedConfig<?> typedConfigFromResult = configTypeContext.newTypedConfig(serviceRanking);
        assertTrue(typedConfigFromResult.getConfig() instanceof ServiceRanking);
        ServiceRanking serviceRanking2 = (ServiceRanking) typedConfigFromResult.getConfig();
        assertEquals(42, serviceRanking2.value());
    }

    @ConfigType(type = ServiceRanking.class, property = "service.ranking:Integer=42", lenient = true)
    public static final class SelfTestSingleElementPass {}

    @Test
    public void testSelfTestSingleElementPass() {
        ConfigType configAnnotation = SelfTestSingleElementPass.class.getAnnotation(ConfigType.class);
        assertNotNull(configTypeContext.constructConfigType(configAnnotation));
    }

    @ConfigType(type = ServiceRanking.class, property = "service.vendor=Acme")
    public static final class SelfTestSingleElementFail {}

    @Test(expected = ConfigTypeStrictnessViolation.class)
    public void testSelfTestSingleElementFail() {
        ConfigType configAnnotation = SelfTestSingleElementFail.class.getAnnotation(ConfigType.class);
        configTypeContext.constructConfigType(configAnnotation);
    }

    @ConfigType(type = ServiceRanking.class, property = "service.vendor=Acme", lenient = true)
    public static final class SelfTestSingleElementLenient {}

    @Test
    public void testSelfTestSingleElementLenient() {
        ConfigType configAnnotation = SelfTestSingleElementLenient.class.getAnnotation(ConfigType.class);
        assertNotNull(configTypeContext.constructConfigType(configAnnotation));
    }

    public @interface ServiceRankingAndVendor {
        int service_ranking();

        String service_vendor() default "";
    }

    @ConfigType(
            type = ServiceRankingAndVendor.class,
            property = {"service.ranking:Integer=42", "service.vendor=Acme"})
    public static final class SelfTestAnnotationPass {}

    @Test
    public void testSelfTestAnnotationPass() {
        ConfigType configAnnotation = SelfTestAnnotationPass.class.getAnnotation(ConfigType.class);
        assertNotNull(configTypeContext.constructConfigType(configAnnotation));
    }

    @ConfigType(
            type = ServiceRankingAndVendor.class,
            property = {"service_ranking:Integer=42", "service.vendor=Acme"})
    public static final class SelfTestAnnotationUnexpectedFail {}

    @Test(expected = ConfigTypeStrictnessViolation.class)
    public void testSelfTestAnnotationUnexpected() {
        ConfigType configAnnotation = SelfTestAnnotationUnexpectedFail.class.getAnnotation(ConfigType.class);
        configTypeContext.constructConfigType(configAnnotation);
    }

    @ConfigType(
            type = ServiceRankingAndVendor.class,
            property = {"service_vendor=Acme"})
    public static final class SelfTestAnnotationMissingFail {}

    @Test(expected = ConfigTypeStrictnessViolation.class)
    public void testSelfTestAnnotationMissing() {
        ConfigType configAnnotation = SelfTestAnnotationMissingFail.class.getAnnotation(ConfigType.class);
        configTypeContext.constructConfigType(configAnnotation);
    }

    public interface IServiceRankingAndVendor {
        int service_ranking();

        String service_vendor();
    }

    @ConfigType(
            type = IServiceRankingAndVendor.class,
            property = {"service.ranking:Integer=42", "service.vendor=Acme"})
    public static final class SelfTestInterfacePass {}

    @Test
    public void testSelfTestInterfacePass() {
        ConfigType configAnnotation = SelfTestInterfacePass.class.getAnnotation(ConfigType.class);
        assertNotNull(configTypeContext.constructConfigType(configAnnotation));
    }

    @ConfigType(
            type = IServiceRankingAndVendor.class,
            property = {"service_ranking:Integer=42", "service_vendor=Acme"})
    public static final class SelfTestInterfaceFail {}

    @Test(expected = ConfigTypeStrictnessViolation.class)
    public void testSelfTestInterfaceFail() {
        ConfigType configAnnotation = SelfTestInterfaceFail.class.getAnnotation(ConfigType.class);
        configTypeContext.constructConfigType(configAnnotation);
    }
}
