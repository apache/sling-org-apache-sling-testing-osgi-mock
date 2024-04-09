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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.testing.mock.osgi.NoScrMetadataException;
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
import org.osgi.util.tracker.ServiceTracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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
        Service3 service = context.registerInjectActivateService(
                Service3.class, "prop1", "value3", ".privateProp", "privateValue");
        assertEquals("value3", service.getConfig().get("prop1"));
        // private key visible in component properties
        assertTrue(service.getConfig().containsKey(".privateProp"));
        assertEquals(Service3.class.getName(), service.getConfig().get("component.name"));
        assertNotNull(service.getConfig().get("component.id"));
        Collection<ServiceReference<ServiceInterface2>> serviceReferences = context.bundleContext()
                .getServiceReferences(ServiceInterface2.class, "(component.name=" + Service3.class.getName() + ")");
        assertEquals("Expected only one service reference matching filter", 1, serviceReferences.size());
        ServiceReference<ServiceInterface2> serviceReference =
                serviceReferences.iterator().next();
        assertEquals("value3", serviceReference.getProperty("prop1"));
        // private key not visible in service properties
        assertNull(serviceReference.getProperty(".privateProp"));
    }

    @Test
    public void testRegisterInjectActivateWithPropertiesWithNulls() {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        Service3 service = context.registerInjectActivateService(
                new Service3(), "prop1", "value3", "prop2", null, null, "value4", null, null);
        assertEquals("value3", service.getConfig().get("prop1"));
    }

    @Test
    public void testRegisterInjectActivateWithPropertyMapNulls() {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        Map<String, Object> props = new HashMap<>();
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

    @Test(expected = RuntimeException.class)
    public void testRegisterInjectActivate_RefrenceMissing_Instance() {
        context.registerInjectActivateService(new Service3());
    }

    @Test(expected = RuntimeException.class)
    public void testRegisterInjectActivate_RefrenceMissing_Class() {
        context.registerInjectActivateService(Service3.class);
    }

    @Test(expected = NoScrMetadataException.class)
    public void testRegisterInjectActivateInvalid_Instance() {
        context.registerInjectActivateService(new Object());
    }

    @Test(expected = NoScrMetadataException.class)
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
}
