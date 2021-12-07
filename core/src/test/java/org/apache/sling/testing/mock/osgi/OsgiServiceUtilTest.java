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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service2;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service3;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service3OsgiR6;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service4;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service5;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface2;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface3;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface5;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceSuperInterface3;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import com.google.common.collect.ImmutableMap;

public class OsgiServiceUtilTest {

    private BundleContext bundleContext = MockOsgi.newBundleContext();
    private Service1 service1;
    private Service2 service2;

    @Before
    public void setUp() {
        service1 = new Service1();
        service2 = new Service2();
        bundleContext.registerService(ServiceInterface1.class.getName(), service1, null);
        bundleContext.registerService(ServiceInterface2.class.getName(), service2, null);
        bundleContext.registerService(ServiceInterface3.class.getName(), service2, null);
    }

    @After
    public void tearDown() {
        MockOsgi.shutdown(bundleContext);
    }

    @Test
    public void testService3() {
        Service3 service3 = new Service3();
        assertTrue(MockOsgi.injectServices(service3, bundleContext));

        Dictionary<String, Object> service3Config = new Hashtable<String, Object>();
        service3Config.put("prop1", "value1");
        assertTrue(MockOsgi.activate(service3, bundleContext, service3Config));

        assertNotNull(service3.getComponentContext());
        assertEquals(service3Config.get("prop1"), service3.getComponentContext().getProperties().get("prop1"));

        assertSame(service1, service3.getReference1());

        List<ServiceInterface2> references2 = service3.getReferences2();
        assertEquals(1, references2.size());
        assertSame(service2, references2.get(0));

        List<ServiceSuperInterface3> references3 = service3.getReferences3();
        assertEquals(1, references3.size());
        assertSame(service2, references3.get(0));

        List<Map<String, Object>> reference3Configs = service3.getReference3Configs();
        assertEquals(1, reference3Configs.size());

        Set<ServiceSuperInterface3> references3Set = service3.getReferences3Set();
        assertEquals(1, references3Set.size());
        assertSame(service2, references3Set.iterator().next());

        assertTrue(MockOsgi.deactivate(service3, bundleContext));
        assertNull(service3.getComponentContext());
    }

    @Test
    public void testService3OsgiR6() {
        Service3OsgiR6 service3 = new Service3OsgiR6();
        assertTrue(MockOsgi.injectServices(service3, bundleContext));

        Dictionary<String, Object> service3Config = new Hashtable<String, Object>();
        service3Config.put("prop1", "value1");
        assertTrue(MockOsgi.activate(service3, bundleContext, service3Config));

        assertNotNull(service3.getComponentContext());
        assertEquals(service3Config.get("prop1"), service3.getComponentContext().getProperties().get("prop1"));

        assertSame(service1, service3.getReference1());

        List<ServiceInterface2> references2 = service3.getReferences2();
        assertEquals(1, references2.size());
        assertSame(service2, references2.get(0));

        List<ServiceSuperInterface3> references3 = service3.getReferences3();
        assertEquals(1, references3.size());
        assertSame(service2, references3.get(0));

        Set<ServiceSuperInterface3> references3Set = service3.getReferences3Set();
        assertEquals(1, references3Set.size());
        assertSame(service2, references3Set.iterator().next());

        Collection<ServiceSuperInterface3> references3Collection = service3.getReferences3Collection();
        assertEquals(1, references3Collection.size());
        assertSame(service2, references3Collection.iterator().next());

        assertTrue(MockOsgi.deactivate(service3, bundleContext));
        assertNull(service3.getComponentContext());
    }

    @Test
    public void testService3_Config() {
        BundleContext bundleContext = MockOsgi.newBundleContext();

        Map<String,Object> initialProperites = ImmutableMap.<String, Object>of("prop1", "value1");

        Service3 service3 = new Service3();
        MockOsgi.activate(service3, bundleContext, initialProperites);
        assertEquals(initialProperites.get("prop1"), service3.getConfig().get("prop1"));

        Map<String,Object> newProperties = ImmutableMap.<String, Object>of("prop2", "value2");
        MockOsgi.modified(service3, bundleContext, newProperties);
        assertEquals(newProperties.get("prop2"), service3.getConfig().get("prop2"));

        newProperties = ImmutableMap.<String, Object>of("prop3", "value3");
        Dictionary<String,Object> newPropertiesDictonary = new Hashtable<String,Object>(newProperties);
        MockOsgi.modified(service3, bundleContext, newPropertiesDictonary);
        assertEquals(newProperties.get("prop3"), service3.getConfig().get("prop3"));

        MockOsgi.modified(service3, bundleContext, "prop3", "value4");
        assertEquals("value4", service3.getConfig().get("prop3"));
    }

    @Test
    public void testService4() {
        Service4 service4 = new Service4();

        assertTrue(MockOsgi.injectServices(service4, bundleContext));
        assertFalse(MockOsgi.activate(service4, bundleContext));

        assertSame(service1, service4.getReference1());
    }

    @Test(expected=NoScrMetadataException.class)
    public void testInjectServicesNoMetadata() {
        MockOsgi.injectServices(new Object(), MockOsgi.newBundleContext());
    }

    @Test(expected=NoScrMetadataException.class)
    public void testActivateNoMetadata() {
        MockOsgi.activate(new Object(), bundleContext);
    }

    @Test(expected=NoScrMetadataException.class)
    public void testDeactivateNoMetadata() {
        MockOsgi.deactivate(new Object(), bundleContext);
    }

    @Test(expected=NoScrMetadataException.class)
    public void testModifiedNoMetadata() {
        MockOsgi.modified(new Object(), MockOsgi.newBundleContext(), ImmutableMap.<String,Object>of());
    }

    @Test
    public void testMockedService() {
        Service5 service5 = Mockito.spy(new Service5());
        Mockito.doReturn(true).when(service5).doRemoteThing();

        MockOsgi.injectServices(service5, bundleContext);
        MockOsgi.activate(service5, bundleContext, (Dictionary<String, Object>) null);
        bundleContext.registerService(ServiceInterface5.class.getName(), service5, null);

        assertSame(service5, bundleContext.getService(
                bundleContext.getServiceReference(ServiceInterface5.class.getName())));
        assertEquals(true, service5.doRemoteThing());
    }

}
