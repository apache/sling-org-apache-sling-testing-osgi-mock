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

import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.NoScrMetadataException;
import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.UpdateConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.apache.sling.testing.mock.osgi.testsvc.osgicontextimpl.MyComponent;
import org.apache.sling.testing.mock.osgi.testsvc.osgicontextimpl.MyService;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service3;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface2;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service8;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.util.tracker.ServiceTracker;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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

    @Before
    public void setUp() throws Exception {
        this.context = new TestOsgiContext();
        this.context.setUpContext();
    }

    @After
    public void tearDown() throws Exception {
        this.context.tearDownContext();
    }

    @ApplyConfig(type = ServiceRanking.class, property = "service.ranking:Integer=42")
    public static class Configured {

    }

    @Test
    public void testApplyConfigToType() {
        ApplyConfig configAnnotation = Configured.class.getAnnotation(ApplyConfig.class);
        Object reified = context.constructComponentPropertyType(configAnnotation);
        assertTrue(reified instanceof ServiceRanking);
        ServiceRanking serviceRanking = (ServiceRanking) reified;
        assertEquals(42, serviceRanking.value());
    }

    @ApplyConfig(type = String.class, property = "service.ranking:Integer=42")
    public static class IllegallyConfigured {

    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplyConfigToTypeThrows() {
        ApplyConfig configAnnotation = IllegallyConfigured.class.getAnnotation(ApplyConfig.class);
        context.constructComponentPropertyType(configAnnotation);
    }

    public @interface ConfigurableFromPid {
        String string_property() default "a default value";
    }

    @ApplyConfig(type = ConfigurableFromPid.class,
            pid = "existing-pid",
            property = {
                    "string.property=a Component.property() value"
            })
    public static class ConfiguredWithPid {

    }

    @Test
    public void testApplyConfigToTypeWithApplyPid() throws Exception {
        final ConfigurationAdmin configurationAdmin = context.getService(ConfigurationAdmin.class);
        assertNotNull(configurationAdmin);
        final Configuration config = configurationAdmin.getConfiguration("existing-pid");
        final ApplyConfig configAnnotation = ConfiguredWithPid.class.getAnnotation(ApplyConfig.class);
        final ApplyConfig rankingAnnotation = Configured.class.getAnnotation(ApplyConfig.class);
        assertEquals("a Component.property() value",
                ((ConfigurableFromPid) context.constructComponentPropertyType(configAnnotation)).string_property());
        assertEquals(42,
                ((ServiceRanking) context.constructComponentPropertyType(rankingAnnotation)).value());
        assertEquals("a Component.property() value",
                ((ConfigurableFromPid) context.constructComponentPropertyType(configAnnotation, "existing-pid")).string_property());
        assertEquals(42,
                ((ServiceRanking) context.constructComponentPropertyType(rankingAnnotation, "existing-pid")).value());

        config.update(MapUtil.toDictionary(Map.of(
                "string.property", "a configured value",
                "service.ranking", 99)));

        assertEquals("a configured value",
                ((ConfigurableFromPid) context.constructComponentPropertyType(configAnnotation)).string_property());
        assertEquals(42,
                ((ServiceRanking) context.constructComponentPropertyType(rankingAnnotation)).value());
        assertEquals("a configured value",
                ((ConfigurableFromPid) context.constructComponentPropertyType(configAnnotation, "existing-pid")).string_property());
        assertEquals(99,
                ((ServiceRanking) context.constructComponentPropertyType(rankingAnnotation, "existing-pid")).value());

        assertEquals("a Component.property() value",
                ((ConfigurableFromPid) context.constructComponentPropertyType(configAnnotation, "new-pid")).string_property());
        assertEquals(42,
                ((ServiceRanking) context.constructComponentPropertyType(rankingAnnotation, "new-pid")).value());
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

    @UpdateConfig(pid = "a pid", property = "service.ranking:Integer=42")
    public static class UpdatesConfiguration {

    }

    @Test
    public void testUpdateConfiguration() throws IOException {
        final ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNull(configAdmin.getConfiguration("a pid").getProperties());
        UpdateConfig updateConfig = UpdatesConfiguration.class.getAnnotation(UpdateConfig.class);
        context.updateConfiguration(updateConfig);
        assertEquals(42, configAdmin.getConfiguration("a pid").getProperties().get("service.ranking"));
    }

    @UpdateConfig(pid = "a pid", property = {
            "service.ranking:Integer=42",
            "service.ranking:Integer=55"
    })
    public static class UpdatesConfigurationArray {

    }

    @Test
    public void testUpdateConfigurationArray() throws IOException {
        final ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNull(configAdmin.getConfiguration("a pid").getProperties());
        UpdateConfig updateConfig = UpdatesConfigurationArray.class.getAnnotation(UpdateConfig.class);
        context.updateConfiguration(updateConfig);
        assertArrayEquals(new Integer[] { 42, 55 },
                (Integer[]) configAdmin.getConfiguration("a pid").getProperties().get("service.ranking"));
    }


    @UpdateConfig(pid = "", property = "service.ranking:Integer=42")
    public static class NoUpdatesConfiguration {

    }

    @Test
    public void testUpdateConfigurationEmptyPid() throws IOException {
        final ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNull(configAdmin.getConfiguration("").getProperties());
        UpdateConfig updateConfig = NoUpdatesConfiguration.class.getAnnotation(UpdateConfig.class);
        context.updateConfiguration(updateConfig);
        assertNull(configAdmin.getConfiguration("").getProperties());
    }

    @UpdateConfig(component = UpdatesConfigurationByComponentName.class,
                property = "service.ranking:Integer=42")
    public static class UpdatesConfigurationByComponentName {

    }

    @Test
    public void testUpdateConfigurationByComponentName() throws IOException {
        final String pid = UpdatesConfigurationByComponentName.class.getName();
        final ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        assertNull(configAdmin.getConfiguration(pid).getProperties());
        UpdateConfig updateConfig = UpdatesConfigurationByComponentName.class.getAnnotation(UpdateConfig.class);
        context.updateConfiguration(updateConfig);
        assertEquals(42, configAdmin.getConfiguration(pid).getProperties().get("service.ranking"));
    }

    @UpdateConfig(pid = "ranking.is.21", property = "service.ranking:Integer=21")
    @UpdateConfig(pid = "ranking.is.42", property = "service.ranking:Integer=42")
    @UpdateConfig(component = AppliesMultipleConfigs.class, property = "service.ranking:Integer=55")
    @ApplyConfig(type = ServiceRanking.class, property = "service.ranking:Integer=10")
    public static class AppliesMultipleConfigs {

    }

    @Test
    public void testApplyConfigToTypeMultiplePids() {
        ConfigAnnotationUtil.findUpdateConfigAnnotations(AppliesMultipleConfigs.class)
                .forEachOrdered(context::updateConfiguration);
        ApplyConfig annotation = AppliesMultipleConfigs.class.getAnnotation(ApplyConfig.class);
        assertEquals(10, ((ServiceRanking) context.constructComponentPropertyType(annotation)).value());
        assertEquals(10, ((ServiceRanking) context.constructComponentPropertyType(annotation, "")).value());
        assertEquals(21, ((ServiceRanking) context.constructComponentPropertyType(annotation, "ranking.is.21")).value());
        assertEquals(42, ((ServiceRanking) context.constructComponentPropertyType(annotation, "ranking.is.42")).value());
        assertEquals(10, ((ServiceRanking) context.constructComponentPropertyType(annotation, "new-pid")).value());
        assertEquals(55, ((ServiceRanking) context.constructComponentPropertyType(annotation, AppliesMultipleConfigs.class.getName())).value());
    }

    @UpdateConfig(pid = "ranking.is.21", property = "service.ranking:Integer=21")
    @UpdateConfig(pid = "ranking.is.42", property = "service.ranking:Integer=42")
    @UpdateConfig(component = AppliesMultipleConfigsWithOwnDefault.class, property = "service.ranking:Integer=55")
    @ApplyConfig(type = ServiceRanking.class, pid = "ranking.is.21",
            property = "service.ranking:Integer=10")
    public static class AppliesMultipleConfigsWithOwnDefault {

    }

    @Test
    public void testApplyConfigToTypeMultiplePidsWithOwnDefault() {
        ConfigAnnotationUtil.findUpdateConfigAnnotations(AppliesMultipleConfigsWithOwnDefault.class)
                .forEachOrdered(context::updateConfiguration);
        ApplyConfig annotation = AppliesMultipleConfigsWithOwnDefault.class.getAnnotation(ApplyConfig.class);
        assertEquals(21, ((ServiceRanking) context.constructComponentPropertyType(annotation)).value());
        assertEquals(21, ((ServiceRanking) context.constructComponentPropertyType(annotation, "")).value());
        assertEquals(42, ((ServiceRanking) context.constructComponentPropertyType(annotation, "ranking.is.42")).value());
        assertEquals(10, ((ServiceRanking) context.constructComponentPropertyType(annotation, "new-pid")).value());
        assertEquals(55, ((ServiceRanking) context.constructComponentPropertyType(annotation, AppliesMultipleConfigsWithOwnDefault.class.getName())).value());
    }

    @UpdateConfig(pid = "ranking.is.21", property = "service.ranking:Integer=21")
    @UpdateConfig(pid = "ranking.is.42", property = "service.ranking:Integer=42")
    @UpdateConfig(component = AppliesMultipleConfigsWithOwnDefaultComponent.class, property = "service.ranking:Integer=55")
    @ApplyConfig(type = ServiceRanking.class, pid = Component.NAME, component = AppliesMultipleConfigsWithOwnDefaultComponent.class,
            property = "service.ranking:Integer=10")
    public static class AppliesMultipleConfigsWithOwnDefaultComponent {

    }

    @Test
    public void testApplyConfigToTypeMultiplePidsWithOwnDefaultComponent() {
        ConfigAnnotationUtil.findUpdateConfigAnnotations(AppliesMultipleConfigsWithOwnDefaultComponent.class)
                .forEachOrdered(context::updateConfiguration);
        ApplyConfig annotation = AppliesMultipleConfigsWithOwnDefaultComponent.class.getAnnotation(ApplyConfig.class);
        assertEquals(55, ((ServiceRanking) context.constructComponentPropertyType(annotation)).value());
        assertEquals(55, ((ServiceRanking) context.constructComponentPropertyType(annotation, "")).value());
        assertEquals(21, ((ServiceRanking) context.constructComponentPropertyType(annotation, "ranking.is.21")).value());
        assertEquals(42, ((ServiceRanking) context.constructComponentPropertyType(annotation, "ranking.is.42")).value());
        assertEquals(10, ((ServiceRanking) context.constructComponentPropertyType(annotation, "new-pid")).value());
        assertEquals(55, ((ServiceRanking) context.constructComponentPropertyType(annotation, AppliesMultipleConfigsWithOwnDefaultComponent.class.getName())).value());
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
        Map<String, Object> existingProps = Map.of(
                "prop.string", "a string",
                "prop.integer", 42);
        doAnswer(call -> MapUtil.toDictionary(Map.copyOf(existingProps))).when(mockedConfig).getProperties();

        ConfigurationAdmin mocked = mock(ConfigurationAdmin.class);
        doReturn(mockedConfig).when(mocked).getConfiguration("existing-pid");

        Map<String, Object> props = new HashMap<>();
        ConfigTypeContext.mergePropertiesFromConfigPid(props, "existing-pid", mocked);
        assertEquals(Map.of(
                "prop.string", "a string",
                "prop.integer", 42), props);

        Map<String, Object> overrideProps = new HashMap<>(Map.of(
                "prop.string", "a different string",
                "prop.boolean", true));

        ConfigTypeContext.mergePropertiesFromConfigPid(overrideProps, "existing-pid", mocked);
        assertEquals(Map.of(
                "prop.string", "a string",
                "prop.boolean", true,
                "prop.integer", 42), overrideProps);
    }

    public interface AnInterface {
        int service_ranking();
    }

    @ApplyConfig(type = AnInterface.class, property = "service.ranking:Integer=42")
    public static class ConfiguredWithInterface {

    }

    @Test
    public void testApplyConfigToInterface() {
        ApplyConfig configAnnotation = ConfiguredWithInterface.class.getAnnotation(ApplyConfig.class);
        Object reified = context.constructComponentPropertyType(configAnnotation);
        assertTrue(reified instanceof AnInterface);
        AnInterface serviceRanking = (AnInterface) reified;
        assertEquals(42, serviceRanking.service_ranking());
    }

    @Test
    public void testNewTypedConfig() {
        ApplyConfig configAnnotation = Configured.class.getAnnotation(ApplyConfig.class);
        TypedConfig<?> typedConfig = AnnotationTypedConfig.newTypedConfig(context, configAnnotation);
        assertTrue(typedConfig.getConfig() instanceof ServiceRanking);
        ServiceRanking serviceRanking = (ServiceRanking) typedConfig.getConfig();
        assertEquals(42, serviceRanking.value());
        TypedConfig<?> typedConfigFromResult = AnnotationTypedConfig.newTypedConfig(context, serviceRanking);
        assertTrue(typedConfigFromResult.getConfig() instanceof ServiceRanking);
        ServiceRanking serviceRanking2 = (ServiceRanking) typedConfigFromResult.getConfig();
        assertEquals(42, serviceRanking2.value());
    }

}
