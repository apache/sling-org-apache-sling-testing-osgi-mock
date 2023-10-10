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
package org.apache.sling.testing.mock.osgi.context;

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

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.NoScrMetadataException;
import org.apache.sling.testing.mock.osgi.config.AnnotationTypedConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigAnnotationUtil;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.UpdateConfig;
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

@SuppressWarnings("null")
public class OsgiContextImplTest {

    private OsgiContextImpl context;

    @Before
    public void setUp() throws Exception {
        this.context = new OsgiContextImpl();
        this.context.setUp();
    }

    @After
    public void tearDown() throws Exception {
        this.context.tearDown();
    }

    @Test
    public void testContextObjects() {
        assertNotNull(context.componentContext());
        assertNotNull(context.bundleContext());
    }

    @Test
    public void testRegisterService() {
        Set<String> myService = new HashSet<String>();
        context.registerService(Set.class, myService);

        Set<?> serviceResult = context.getService(Set.class);
        assertSame(myService, serviceResult);
    }

    @Test
    public void testRegisterServiceWithProperties() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");

        Set<String> myService = new HashSet<String>();
        context.registerService(Set.class, myService, props);

        ServiceReference<?> serviceReference = context.bundleContext().getServiceReference(Set.class.getName());
        Object serviceResult = context.bundleContext().getService(serviceReference);
        assertSame(myService, serviceResult);
        assertEquals("value1", serviceReference.getProperty("prop1"));
    }

    @Test
    public void testRegisterServiceWithPropertiesVarargs() {
        Set<String> myService = new HashSet<String>();
        context.registerService(Set.class, myService, "prop1", "value1");

        ServiceReference<?> serviceReference = context.bundleContext().getServiceReference(Set.class.getName());
        Object serviceResult = context.bundleContext().getService(serviceReference);
        assertSame(myService, serviceResult);
        assertEquals("value1", serviceReference.getProperty("prop1"));
    }

    @Test
    public void testRegisterMultipleServices() {
        Set[] serviceResults = context.getServices(Set.class, null);
        assertEquals(0, serviceResults.length);

        Set<String> myService1 = new HashSet<String>();
        context.registerService(Set.class, myService1);
        Set<String> myService2 = new HashSet<String>();
        context.registerService(Set.class, myService2);

        assertSame(myService1, context.getService(Set.class));

        // expected: ascending order because ordering ascending by service ID
        serviceResults = context.getServices(Set.class, null);
        assertEquals(2, serviceResults.length);
        assertSame(myService1, serviceResults[0]);
        assertSame(myService2, serviceResults[1]);
    }

    @Test
    public void testRegisterInjectActivate_Instance() {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        Service3 service = context.registerInjectActivateService(new Service3());
        assertNotNull(service.getReference1());
        assertEquals(2, service.getReferences2().size());
        assertEquals(Service3.class.getName(), service.getConfig().get("component.name"));
        assertNotNull(service.getConfig().get("component.id"));
    }

    @Test
    public void testRegisterInjectActivate_Class() {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        Service3 service = context.registerInjectActivateService(Service3.class);
        assertNotNull(service.getReference1());
        assertEquals(2, service.getReferences2().size());
        assertEquals(Service3.class.getName(), service.getConfig().get("component.name"));
        assertNotNull(service.getConfig().get("component.id"));
    }

    @Test
    public void testRegisterInjectActivateWithProperties_Instance() {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        Service3 service = context.registerInjectActivateService(new Service3(), "prop1", "value3");
        assertEquals("value3", service.getConfig().get("prop1"));
    }

    @Test
    public void testRegisterInjectActivateWithProperties_Class() throws InvalidSyntaxException {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        Service3 service = context.registerInjectActivateService(Service3.class, "prop1", "value3", ".privateProp", "privateValue");
        assertEquals("value3", service.getConfig().get("prop1"));
        // private key visible in component properties
        assertTrue(service.getConfig().containsKey(".privateProp"));
        assertEquals(Service3.class.getName(), service.getConfig().get("component.name"));
        assertNotNull(service.getConfig().get("component.id"));
        Collection<ServiceReference<ServiceInterface2>> serviceReferences = context.bundleContext().getServiceReferences(ServiceInterface2.class, "(component.name="+Service3.class.getName()+")");
        assertEquals("Expected only one service reference matching filter", 1, serviceReferences.size());
        ServiceReference<ServiceInterface2> serviceReference = serviceReferences.iterator().next();
        assertEquals("value3", serviceReference.getProperty("prop1"));
        // private key not visible in service properties
        assertNull(serviceReference.getProperty(".privateProp"));
    }

    @Test
    public void testRegisterInjectActivateWithPropertiesWithNulls() {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        Service3 service = context.registerInjectActivateService(new Service3(),
                "prop1", "value3",
                "prop2", null,
                null, "value4",
                null, null);
        assertEquals("value3", service.getConfig().get("prop1"));
    }

    @Test
    public void testRegisterInjectActivateWithPropertyMapNulls() {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        Map<String,Object> props = new HashMap<>();
        props.put("prop1", "value3");
        props.put("prop2", null);
        props.put(null, "value4");
        props.put(null, null);
        Service3 service = context.registerInjectActivateService(new Service3(), props);
        assertEquals("value3", service.getConfig().get("prop1"));
        assertEquals(Service3.class.getName(), service.getConfig().get("component.name"));
        assertNotNull(service.getConfig().get("component.id"));
    }

    @Test
    public void testRegisterInjectActivateWithAdditionalManualServiceRegistration() {
        context.registerInjectActivateService(new Service8());
    }

    @Test(expected=RuntimeException.class)
    public void testRegisterInjectActivate_RefrenceMissing_Instance() {
        context.registerInjectActivateService(new Service3());
    }

    @Test(expected=RuntimeException.class)
    public void testRegisterInjectActivate_RefrenceMissing_Class() {
        context.registerInjectActivateService(Service3.class);
    }

    @Test(expected=NoScrMetadataException.class)
    public void testRegisterInjectActivateInvalid_Instance() {
        context.registerInjectActivateService(new Object());
    }

    @Test(expected=NoScrMetadataException.class)
    public void testRegisterInjectActivateInvalid_Class() {
        context.registerInjectActivateService(Object.class);
    }

    @Test
    public void testServiceTracker() {
        BundleContext bundleContext = context.bundleContext();
        ServiceTracker<MyService, MyService> tracker = new ServiceTracker<>(bundleContext, MyService.class, null);
        tracker.open();

        context.registerInjectActivateService(new MyComponent());

        assertNotNull(tracker.getServiceReferences());
        assertEquals(1, tracker.getServiceReferences().length);

        tracker.close();
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
        OsgiContextImpl.updatePropertiesForConfigPid(props, "new-pid", mocked);
    }

    @Test
    public void testUpdatePropertiesForNewConfiguration() throws IOException {
        ConfigurationAdmin mocked = mock(ConfigurationAdmin.class);
        Configuration mockedConfig = mock(Configuration.class);
        doReturn(mockedConfig).when(mocked).getConfiguration("new-pid");
        Map<String, Object> props = Map.of("new.property", "value");
        final Dictionary<String, Object> asDict = MapUtil.toDictionary(props);
        OsgiContextImpl.updatePropertiesForConfigPid(props, "new-pid", mocked);
        verify(mockedConfig, times(1)).update(asDict);
    }

    @Test
    public void testUpdatePropertiesForNullConfigurationAdmin() {
        Map<String, Object> props = new HashMap<>();
        // no throws
        OsgiContextImpl.updatePropertiesForConfigPid(props, "new-pid", null);
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
        OsgiContextImpl.mergePropertiesFromConfigPid(props, "new-pid", mocked);
    }

    @Test
    public void testMergePropertiesFromNewConfiguration() throws IOException {
        ConfigurationAdmin mocked = mock(ConfigurationAdmin.class);
        Configuration mockedConfig = mock(Configuration.class);
        doReturn(mockedConfig).when(mocked).getConfiguration("new-pid");
        Map<String, Object> props = new HashMap<>();
        OsgiContextImpl.mergePropertiesFromConfigPid(props, "new-pid", mocked);
        assertTrue(props.isEmpty());
    }

    @Test
    public void testMergePropertiesFromNullConfigurationAdmin() throws IOException {
        Map<String, Object> props = new HashMap<>();
        OsgiContextImpl.mergePropertiesFromConfigPid(props, "new-pid", null);
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
        OsgiContextImpl.mergePropertiesFromConfigPid(props, "existing-pid", mocked);
        assertEquals(Map.of(
                "prop.string", "a string",
                "prop.integer", 42), props);

        Map<String, Object> overrideProps = new HashMap<>(Map.of(
                "prop.string", "a different string",
                "prop.boolean", true));

        OsgiContextImpl.mergePropertiesFromConfigPid(overrideProps, "existing-pid", mocked);
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
