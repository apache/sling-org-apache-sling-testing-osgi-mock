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

import java.util.Map;
import java.util.stream.Stream;

import org.apache.sling.testing.mock.osgi.config.annotations.ConfigCollection;
import org.apache.sling.testing.mock.osgi.config.annotations.ConfigType;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

@ConfigType(type = ServiceRanking.class, lenient = true)
public class AnnotationTypedConfigTest {
    private TestOsgiContext context;
    private ConfigTypeContext configTypeContext;

    @ConfigType(type = ServiceVendor.class, lenient = true)
    static class TestOsgiContext extends OsgiContextImpl {

        void setUpContext() {
            this.setUp();
        }

        void tearDownContext() {
            this.tearDown();
        }
    }

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

    @Test
    public void testNewInstance() {
        final ConfigType annotation = getClass().getAnnotation(ConfigType.class);
        final ServiceRanking config = (ServiceRanking) configTypeContext.constructConfigType(annotation);
        final TypedConfig<ServiceRanking> typedConfig =
                AnnotationTypedConfig.newInstance(ServiceRanking.class, config, annotation);
        assertSame(ServiceRanking.class, typedConfig.getType());
        assertSame(config, typedConfig.getConfig());
        assertEquals(0, typedConfig.stream(ServiceVendor.class).count());
    }

    @Test
    public void testNewInstanceUsingConfigAsAnnotation() {
        final ConfigType annotation = getClass().getAnnotation(ConfigType.class);
        final ServiceRanking config = (ServiceRanking) configTypeContext.constructConfigType(annotation);
        final TypedConfig<ServiceRanking> typedConfig =
                AnnotationTypedConfig.newInstance(ServiceRanking.class, config, config);
        assertSame(ServiceRanking.class, typedConfig.getType());
        assertSame(config, typedConfig.getConfig());
        assertEquals(0, typedConfig.stream(ServiceVendor.class).count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeCastMismatch() throws Exception {
        ConfigType annotation = getClass().getAnnotation(ConfigType.class);
        final ServiceRanking config = (ServiceRanking) configTypeContext.constructConfigType(annotation);
        AnnotationTypedConfig.newInstance(ServiceVendor.class, config, annotation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeAnnotationMismatch() throws Exception {
        ConfigType annotation = getClass().getAnnotation(ConfigType.class);
        final ServiceRanking config = (ServiceRanking) configTypeContext.constructConfigType(annotation);
        ConfigType wrongAnnotation = TestOsgiContext.class.getAnnotation(ConfigType.class);
        AnnotationTypedConfig.newInstance(ServiceRanking.class, config, wrongAnnotation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeAnnotationMismatchUsingConfigAsAnnotation() throws Exception {
        ConfigType annotation = getClass().getAnnotation(ConfigType.class);
        final ServiceRanking config = (ServiceRanking) configTypeContext.constructConfigType(annotation);
        ConfigType wrongAnnotation = TestOsgiContext.class.getAnnotation(ConfigType.class);
        final ServiceVendor wrongAnnotationConfig =
                (ServiceVendor) configTypeContext.constructConfigType(wrongAnnotation);
        AnnotationTypedConfig.newInstance(ServiceRanking.class, config, wrongAnnotationConfig);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetConfigMap() {
        ConfigType annotation = getClass().getAnnotation(ConfigType.class);
        assertEquals(
                Map.of("service.ranking", 0),
                configTypeContext.newTypedConfig(annotation).getConfigMap());
        ConfigType wrongAnnotation = TestOsgiContext.class.getAnnotation(ConfigType.class);
        final ServiceVendor wrongAnnotationConfig =
                (ServiceVendor) configTypeContext.constructConfigType(wrongAnnotation);
        assertTrue(AbstractConfigTypeReflectionProvider.getInstance(annotation.type())
                .getPropertyMap(wrongAnnotationConfig)
                .isEmpty());

        ConfigCollection collection = mock(ConfigCollection.class);
        doCallRealMethod().when(collection).stream(any(Class.class));
        doCallRealMethod().when(collection).firstConfigMap(any(Class.class));
        doAnswer(call -> Stream.of(configTypeContext.newTypedConfig(annotation))).when(collection).stream();

        assertEquals(Map.of("service.ranking", 0), collection.firstConfigMap(ServiceRanking.class));
    }
}
