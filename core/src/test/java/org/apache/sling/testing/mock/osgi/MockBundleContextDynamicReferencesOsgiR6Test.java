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
package org.apache.sling.testing.mock.osgi;

import java.util.Map;
import java.util.Set;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service3OsgiR6;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service3OsgiR6Impl;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1Optional;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface2;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface3;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceSuperInterface3;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@RunWith(MockitoJUnitRunner.class)
public class MockBundleContextDynamicReferencesOsgiR6Test {

    private BundleContext bundleContext;
    private Service3OsgiR6 service;
    private ServiceRegistration reg1a;
    private ServiceRegistration reg2a;

    @Mock
    private ServiceInterface1 dependency1a;

    @Mock
    private ServiceInterface1 dependency1b;

    @Mock
    private ServiceInterface1Optional dependency1aOptional;

    @Mock
    private ServiceInterface1Optional dependency1bOptional;

    @Mock
    private ServiceInterface2 dependency2a;

    @Mock
    private ServiceInterface2 dependency2b;

    @Mock
    private ServiceSuperInterface3 dependency3a;

    @Mock
    private ServiceSuperInterface3 dependency3b;

    @Mock
    private ServiceSuperInterface3 dependency3c;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();

        // setup service instance with only minimum mandatory references
        reg1a = bundleContext.registerService(ServiceInterface1.class.getName(), dependency1a, null);
        reg2a = bundleContext.registerService(ServiceInterface2.class.getName(), dependency2a, null);

        service = newService3OsgiR6();
        MockOsgi.injectServices(service, bundleContext);
        MockOsgi.activate(service, bundleContext);
        bundleContext.registerService(
                Service3OsgiR6.class.getName(),
                service,
                MapUtil.toDictionary(Map.<String, Object>of("reference3DynamicFiltered.target", "(prop1=def)")));

        assertDependency1(dependency1a);
        assertDependency1Optional(null);
        assertDependencies2(dependency2a);
        assertDependencies3();
    }

    protected Service3OsgiR6 newService3OsgiR6() {
        return new Service3OsgiR6Impl();
    }

    @Test
    public void testAddRemoveOptionalUnaryService() {
        ServiceRegistration reg1aOptional =
                bundleContext.registerService(ServiceInterface1Optional.class.getName(), dependency1aOptional, null);
        assertDependency1Optional(dependency1aOptional);

        reg1aOptional.unregister();
        assertDependency1Optional(null);
    }

    public void testAddOptionalUnaryService_TooMany() {
        bundleContext.registerService(ServiceInterface1Optional.class.getName(), dependency1aOptional, null);
        assertDependency1Optional(dependency1aOptional);

        // in real OSGi this should fail - but this is not covered by the current implementation. so test the real
        // implementation here.
        bundleContext.registerService(ServiceInterface1Optional.class.getName(), dependency1bOptional, null);
        assertDependency1Optional(dependency1bOptional);
    }

    @Test
    public void testAddMandatoryUnaryService_TooMany() {
        // should not throw an exception although mandatory unary reference is already set
        bundleContext.registerService(ServiceInterface1.class.getName(), dependency1b, null);
    }

    @Test
    public void testRemoveMandatoryUnaryService_TooMany() {
        // this should check if the mandatory references is no longer set - but this is currently not implemented
        reg1a.unregister();
    }

    @Test
    public void testAddRemoveOptionalMultipleService() {
        ServiceRegistration reg3a =
                bundleContext.registerService(ServiceInterface3.class.getName(), dependency3a, null);
        assertDependencies3(dependency3a);

        ServiceRegistration reg3b =
                bundleContext.registerService(ServiceInterface3.class.getName(), dependency3b, null);
        assertDependencies3(dependency3a, dependency3b);

        reg3a.unregister();
        assertDependencies3(dependency3b);

        reg3b.unregister();
        assertDependencies3();
    }

    @Test
    public void testAddRemoveMandatoryMultipleService() {
        ServiceRegistration reg2b =
                bundleContext.registerService(ServiceInterface2.class.getName(), dependency2b, null);
        assertDependencies2(dependency2a, dependency2b);

        reg2b.unregister();
        assertDependencies2(dependency2a);

        // in real OSGi this should fail - but this is not covered by the current implementation. so test the real
        // implementation here.
        reg2a.unregister();
        assertDependencies2();
    }

    @Test
    public void testReferenceWithTargetFilter() {
        assertDependencies3Filtered();

        bundleContext.registerService(
                ServiceInterface3.class.getName(),
                dependency3a,
                MapUtil.toDictionary(Map.<String, Object>of("prop1", "abc")));

        bundleContext.registerService(
                ServiceInterface3.class.getName(),
                dependency3b,
                MapUtil.toDictionary(Map.<String, Object>of("prop1", "def")));

        assertDependencies3Filtered(dependency3a);
    }

    @Test
    public void testReferenceWithDynamicTargetFilter() {
        assertDependencies3DynamicFiltered(null);

        bundleContext.registerService(
                ServiceSuperInterface3.class.getName(),
                dependency3a,
                MapUtil.toDictionary(Map.<String, Object>of("prop1", "abc")));

        bundleContext.registerService(
                ServiceSuperInterface3.class.getName(),
                dependency3b,
                MapUtil.toDictionary(Map.<String, Object>of("prop1", "def")));

        bundleContext.registerService(
                ServiceSuperInterface3.class.getName(),
                dependency3c,
                MapUtil.toDictionary(Map.<String, Object>of("prop1", "hij")));

        assertDependencies3DynamicFiltered(dependency3b);
    }

    private void assertDependency1(ServiceInterface1 instance) {
        if (instance == null) {
            assertNull(service.getReference1());
        } else {
            assertSame(instance, service.getReference1());
        }
    }

    private void assertDependency1Optional(ServiceInterface1Optional instance) {
        if (instance == null) {
            assertNull(service.getReference1Optional());
        } else {
            assertSame(instance, service.getReference1Optional());
        }
    }

    private void assertDependencies2(ServiceInterface2... instances) {
        assertEquals(Set.<ServiceInterface2>of(instances), Set.<ServiceInterface2>copyOf(service.getReferences2()));
    }

    private void assertDependencies3(ServiceSuperInterface3... instances) {
        assertEquals(
                Set.<ServiceSuperInterface3>of(instances),
                Set.<ServiceSuperInterface3>copyOf(service.getReferences3()));
    }

    private void assertDependencies3Filtered(ServiceSuperInterface3... instances) {
        assertEquals(
                Set.<ServiceSuperInterface3>of(instances),
                Set.<ServiceSuperInterface3>copyOf(service.getReferences3Filtered()));
    }

    private void assertDependencies3DynamicFiltered(ServiceSuperInterface3 instance) {
        assertEquals(instance, service.getReference3DynamicFiltered());
    }
}
