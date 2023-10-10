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

import org.apache.sling.testing.mock.osgi.config.annotations.ApplyConfig;
import org.apache.sling.testing.mock.osgi.config.annotations.TypedConfig;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


@ApplyConfig(type = ServiceRanking.class)
public class AnnotationTypedConfigTest {
    private TestOsgiContext context;

    @ApplyConfig(type = ServiceVendor.class)
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
    }

    @After
    public void tearDown() throws Exception {
        this.context.tearDownContext();
    }

    @Test
    public void testNewInstance() {
        final ApplyConfig annotation = getClass().getAnnotation(ApplyConfig.class);
        final ServiceRanking config = (ServiceRanking) context.constructComponentPropertyType(annotation);
        final TypedConfig<ServiceRanking> typedConfig = AnnotationTypedConfig.newInstance(ServiceRanking.class,
                config, annotation);
        assertSame(ServiceRanking.class, typedConfig.getType());
        assertSame(config, typedConfig.getConfig());
        assertEquals(0, typedConfig.stream(ServiceVendor.class).count());
    }

    @Test
    public void testNewInstanceUsingConfigAsAnnotation() {
        final ApplyConfig annotation = getClass().getAnnotation(ApplyConfig.class);
        final ServiceRanking config = (ServiceRanking) context.constructComponentPropertyType(annotation);
        final TypedConfig<ServiceRanking> typedConfig = AnnotationTypedConfig.newInstance(ServiceRanking.class,
                config, config);
        assertSame(ServiceRanking.class, typedConfig.getType());
        assertSame(config, typedConfig.getConfig());
        assertEquals(0, typedConfig.stream(ServiceVendor.class).count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeCastMismatch() throws Exception {
        ApplyConfig annotation = getClass().getAnnotation(ApplyConfig.class);
        final ServiceRanking config = (ServiceRanking) context.constructComponentPropertyType(annotation);
        AnnotationTypedConfig.newInstance(ServiceVendor.class, config, annotation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeAnnotationMismatch() throws Exception {
        ApplyConfig annotation = getClass().getAnnotation(ApplyConfig.class);
        final ServiceRanking config = (ServiceRanking) context.constructComponentPropertyType(annotation);
        ApplyConfig wrongAnnotation = TestOsgiContext.class.getAnnotation(ApplyConfig.class);
        AnnotationTypedConfig.newInstance(ServiceRanking.class, config, wrongAnnotation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeAnnotationMismatchUsingConfigAsAnnotation() throws Exception {
        ApplyConfig annotation = getClass().getAnnotation(ApplyConfig.class);
        final ServiceRanking config = (ServiceRanking) context.constructComponentPropertyType(annotation);
        ApplyConfig wrongAnnotation = TestOsgiContext.class.getAnnotation(ApplyConfig.class);
        final ServiceVendor wrongAnnotationConfig = (ServiceVendor) context.constructComponentPropertyType(wrongAnnotation);
        AnnotationTypedConfig.newInstance(ServiceRanking.class, config, wrongAnnotationConfig);
    }
}