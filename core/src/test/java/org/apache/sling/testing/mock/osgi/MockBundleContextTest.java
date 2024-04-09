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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("null")
public class MockBundleContextTest {

    private BundleContext bundleContext;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
    }

    @After
    public void tearDown() {
        MockOsgi.shutdown(bundleContext);
    }

    @Test
    public void testBundle() {
        assertNotNull(bundleContext.getBundle());
    }

    @Test
    public void testServiceRegistration() throws InvalidSyntaxException {
        // prepare test services
        String[] clazzes1 = new String[] {String.class.getName(), Integer.class.getName()};
        Object service1 = new Object();
        Dictionary<String, Object> properties2 = ranking(null);
        ServiceRegistration reg1 = bundleContext.registerService(clazzes1, service1, properties2);

        String clazz2 = String.class.getName();
        Object service2 = new Object();
        Dictionary<String, Object> properties1 = ranking(null);
        ServiceRegistration reg2 = bundleContext.registerService(clazz2, service2, properties1);

        String clazz3 = Integer.class.getName();
        Object service3 = new Object();
        Dictionary<String, Object> properties3 = ranking(-100);
        ServiceRegistration reg3 = bundleContext.registerService(clazz3, service3, properties3);

        // test get service references
        ServiceReference<?> refString = bundleContext.getServiceReference(String.class.getName());
        assertSame(reg1.getReference(), refString);

        ServiceReference<?> refInteger = bundleContext.getServiceReference(Integer.class.getName());
        assertSame(reg1.getReference(), refInteger);

        ServiceReference<?>[] refsString = bundleContext.getServiceReferences(String.class.getName(), null);
        assertEquals(2, refsString.length);
        assertSame(reg1.getReference(), refsString[0]);
        assertSame(reg2.getReference(), refsString[1]);

        Collection<ServiceReference<String>> refColString = bundleContext.getServiceReferences(String.class, null);
        assertEquals(2, refColString.size());
        assertSame(reg1.getReference(), refColString.iterator().next());

        ServiceReference<?>[] refsInteger = bundleContext.getServiceReferences(Integer.class.getName(), null);
        assertEquals(2, refsInteger.length);
        assertSame(reg1.getReference(), refsInteger[0]);
        assertSame(reg3.getReference(), refsInteger[1]);

        ServiceReference<?>[] allRefsString = bundleContext.getAllServiceReferences(String.class.getName(), null);
        assertArrayEquals(refsString, allRefsString);

        // test get services
        assertSame(service1, bundleContext.getService(refsString[0]));
        assertSame(service2, bundleContext.getService(refsString[1]));
        assertSame(service1, bundleContext.getService(refInteger));

        // unget does nothing
        bundleContext.ungetService(refsString[0]);
        bundleContext.ungetService(refsString[1]);
        bundleContext.ungetService(refInteger);
    }

    @Test
    public void testModifyServiceRegistration() throws InvalidSyntaxException {
        // register test services
        String service1 = "service";
        Dictionary<String, Object> properties1 = new Hashtable<>();
        properties1.put("a", "1");
        properties1.put("b", "2");

        final List<ServiceEvent> events = new ArrayList<>();
        final ServiceListener listener = new ServiceListener() {

            @Override
            public void serviceChanged(ServiceEvent event) {
                events.add(event);
            }
        };
        bundleContext.addServiceListener(listener);
        ServiceRegistration<String> reg1 = bundleContext.registerService(String.class, service1, properties1);

        // check properties for registration
        assertEquals(4, reg1.getReference().getPropertyKeys().length);
        assertNotNull(reg1.getReference().getProperty(Constants.SERVICE_ID));
        assertNotNull(reg1.getReference().getProperty(Constants.OBJECTCLASS));
        assertEquals("1", reg1.getReference().getProperty("a"));
        assertEquals("2", reg1.getReference().getProperty("b"));

        // check for registered event
        assertEquals(1, events.size());
        assertEquals(ServiceEvent.REGISTERED, events.get(0).getType());
        assertSame(reg1.getReference(), events.get(0).getServiceReference());

        // update properties
        Dictionary<String, Object> properties2 = new Hashtable<>();
        properties2.put("a", "1");
        properties2.put("c", "3");
        reg1.setProperties(properties2);

        // check properties
        assertEquals(4, reg1.getReference().getPropertyKeys().length);
        assertNotNull(reg1.getReference().getProperty(Constants.SERVICE_ID));
        assertNotNull(reg1.getReference().getProperty(Constants.OBJECTCLASS));
        assertEquals("1", reg1.getReference().getProperty("a"));
        assertEquals("3", reg1.getReference().getProperty("c"));

        // check for modified event
        assertEquals(2, events.size());
        assertEquals(ServiceEvent.MODIFIED, events.get(1).getType());
        assertSame(reg1.getReference(), events.get(1).getServiceReference());

        // unregister
        reg1.unregister();

        // check for unregister event
        assertEquals(3, events.size());
        assertEquals(ServiceEvent.UNREGISTERING, events.get(2).getType());
        assertSame(reg1.getReference(), events.get(2).getServiceReference());
    }

    @Test
    public void testServiceFactoryRegistration() throws InvalidSyntaxException {
        // prepare test services
        Class<String> clazz = String.class;
        final String service = "abc";
        Dictionary<String, Object> properties1 = ranking(null);
        ServiceRegistration reg = bundleContext.registerService(
                clazz,
                new ServiceFactory<String>() {
                    @Override
                    public String getService(Bundle bundle, ServiceRegistration<String> registration) {
                        return service;
                    }

                    @Override
                    public void ungetService(Bundle bundle, ServiceRegistration<String> registration, String service) {
                        // do nothing
                    }
                },
                properties1);

        ServiceReference<String> ref = bundleContext.getServiceReference(clazz);
        assertNotNull(ref);
        assertSame(reg.getReference(), ref);
        assertSame(service, bundleContext.getService(ref));
        bundleContext.ungetService(ref);
    }

    @Test
    public void testNoServiceReferences() throws InvalidSyntaxException {
        ServiceReference<?>[] refs = bundleContext.getServiceReferences(String.class.getName(), null);
        assertNull(refs);

        Collection<ServiceReference<String>> refCol = bundleContext.getServiceReferences(String.class, null);
        assertNotNull(refCol);
        assertTrue(refCol.isEmpty());
    }

    @Test
    public void testServiceUnregistration() {
        // prepare test services
        String clazz1 = String.class.getName();
        Object service1 = new Object();
        Dictionary<String, Object> properties1 = ranking(null);
        ServiceRegistration reg1 = bundleContext.registerService(clazz1, service1, properties1);

        assertNotNull(bundleContext.getServiceReference(clazz1));

        reg1.unregister();

        assertNull(bundleContext.getServiceReference(clazz1));

        try {
            reg1.unregister();
            Assert.fail("Unregistering a non existant service should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Service was already unregistered", e.getMessage());
        }
    }

    @Test
    public void testGetBundles() throws Exception {
        assertEquals(0, bundleContext.getBundles().length);
    }

    @Test
    public void testServiceListener() throws Exception {
        ServiceListener serviceListener = mock(ServiceListener.class);
        bundleContext.addServiceListener(serviceListener);

        // prepare test services
        String clazz1 = String.class.getName();
        Object service1 = new Object();
        bundleContext.registerService(clazz1, service1, null);

        verify(serviceListener).serviceChanged(any(ServiceEvent.class));

        bundleContext.removeServiceListener(serviceListener);
    }

    @Test
    public void testBundleListener() throws Exception {
        BundleListener bundleListener = mock(BundleListener.class);
        BundleEvent bundleEvent = mock(BundleEvent.class);

        bundleContext.addBundleListener(bundleListener);

        MockOsgi.sendBundleEvent(bundleContext, bundleEvent);
        verify(bundleListener).bundleChanged(bundleEvent);

        bundleContext.removeBundleListener(bundleListener);
    }

    @Test
    public void testFrameworkListener() throws Exception {
        // ensure that listeners can be called (although they are not expected
        // to to anything)
        bundleContext.addFrameworkListener(null);
        bundleContext.removeFrameworkListener(null);
    }

    @Test
    public void testGetProperty() {
        String propName = this.getClass().getName();
        System.setProperty(propName, "random");
        try {
            assertEquals("random", bundleContext.getProperty(propName));
        } finally {
            System.getProperties().remove(propName);
        }
        assertNull(bundleContext.getProperty(propName));
    }

    @Test
    public void testObjectClassFilterMatches() throws InvalidSyntaxException {

        Filter filter = bundleContext.createFilter("(" + Constants.OBJECTCLASS + "=" + Integer.class.getName() + ")");

        ServiceRegistration serviceRegistration =
                bundleContext.registerService(Integer.class.getName(), Integer.valueOf(1), null);

        assertTrue(filter.match(serviceRegistration.getReference()));
    }

    @Test
    public void testObjectClassFilterDoesNotMatch() throws InvalidSyntaxException {

        Filter filter = bundleContext.createFilter("(" + Constants.OBJECTCLASS + "=" + Integer.class.getName() + ")");

        ServiceRegistration serviceRegistration =
                bundleContext.registerService(Long.class.getName(), Long.valueOf(1), null);

        assertFalse(filter.match(serviceRegistration.getReference()));
    }

    @Test
    public void testGetDataFile() {
        File rootFile = bundleContext.getDataFile("");
        assertNotNull(rootFile);

        File childFile = bundleContext.getDataFile("child");
        assertNotNull(childFile);

        assertEquals(childFile.getParentFile(), rootFile);
    }

    @Test
    public void testSystemBundleById() {
        Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_ID);
        assertNotNull(systemBundle);
        assertEquals(Constants.SYSTEM_BUNDLE_ID, systemBundle.getBundleId());
        assertEquals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, systemBundle.getSymbolicName());
        assertEquals(Constants.SYSTEM_BUNDLE_LOCATION, systemBundle.getLocation());
    }

    @Test
    public void testSystemBundleByLocation() {
        Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        assertNotNull(systemBundle);
        assertEquals(Constants.SYSTEM_BUNDLE_ID, systemBundle.getBundleId());
        assertEquals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, systemBundle.getSymbolicName());
        assertEquals(Constants.SYSTEM_BUNDLE_LOCATION, systemBundle.getLocation());
    }

    @Test
    public void testGetServiceOrderWithRanking() {
        bundleContext.registerService(String.class, "service1", ranking(10));
        bundleContext.registerService(String.class, "service2", ranking(20));
        bundleContext.registerService(String.class, "service3", ranking(5));

        // should return service with highest ranking
        ServiceReference<String> ref = bundleContext.getServiceReference(String.class);
        String service = bundleContext.getService(ref);
        assertEquals("service2", service);

        bundleContext.ungetService(ref);
    }

    @Test
    public void testGetServiceOrderWithoutRanking() {
        bundleContext.registerService(String.class, "service1", ranking(null));
        bundleContext.registerService(String.class, "service2", ranking(null));
        bundleContext.registerService(String.class, "service3", ranking(null));

        // should return service with lowest service id = which was registered first
        ServiceReference<String> ref = bundleContext.getServiceReference(String.class);
        String service = bundleContext.getService(ref);
        assertEquals("service1", service);

        bundleContext.ungetService(ref);
    }

    @Test
    public void testGetServicesWithNoClassOnlyFilter() throws InvalidSyntaxException {
        bundleContext.registerService(String.class, "service1", testProperty());
        bundleContext.registerService(Long.class, Long.valueOf(2), testProperty());
        bundleContext.registerService(Integer.class, Integer.valueOf(9), testProperty());

        // should return service with lowest service id = which was registered first
        ServiceReference[] refs = bundleContext.getServiceReferences((String) null, "(prop1=value1)");
        assertNotNull(refs);
        assertEquals(3, refs.length);
    }

    private static Dictionary<String, Object> ranking(final Integer serviceRanking) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        if (serviceRanking != null) {
            props.put(Constants.SERVICE_RANKING, serviceRanking);
        }
        return props;
    }

    private static Dictionary<String, Object> testProperty() {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("prop1", "value1");
        return props;
    }
}
