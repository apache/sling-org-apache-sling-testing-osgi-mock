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
import java.util.regex.Pattern;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service1Constructor;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service2;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service2Constructor;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service3;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service3Constructor;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service4;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service4Constructor;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service5;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service5Constructor;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service6;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service6Constructor;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service7;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service7Constructor;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service9;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.ServiceReferenceInConstructor;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test different variants of activate/deactivate methods with varying signatures.
 */
public class OsgiServiceUtilActivateDeactivateTest {

    private Map<String, Object> map =
            Map.<String, Object>of("prop1", "value1", "prop2.with.periods", "value2", "prop3-with-hyphens", "value3");
    private BundleContext bundleContext = MockOsgi.newBundleContext();

    @Test
    public void testService1() {
        Service1 service = new Service1();

        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService1Constructor() {
        Service1Constructor service = MockOsgi.activateInjectServices(Service1Constructor.class, bundleContext, map);

        assertNotNull(service);
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService2() {
        Service2 service = new Service2();

        assertTrue(MockOsgi.activate(service, bundleContext, "prop1", "value1"));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getBundleContext());

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService2Constructor() {
        Service2Constructor service =
                MockOsgi.activateInjectServices(Service2Constructor.class, bundleContext, "prop1", "value1");

        assertNotNull(service);
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getBundleContext());

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService3() {
        Service3 service = new Service3();

        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertEquals("value1", service.getMap().get("prop1"));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService3Constructor() {
        Service3Constructor service = MockOsgi.activateInjectServices(Service3Constructor.class, bundleContext, map);

        assertNotNull(service);
        assertTrue(service.isActivated());
        assertEquals("value1", service.getMap().get("prop1"));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService4() {
        Service4 service = new Service4();

        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertEquals(map, Map.copyOf(service.getMap()));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService4Constructor() {
        Service4Constructor service = MockOsgi.activateInjectServices(Service4Constructor.class, bundleContext, map);

        assertNotNull(service);
        assertTrue(service.isActivated());
        assertEquals(map, Map.copyOf(service.getMap()));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService5() {
        Service5 service = new Service5();

        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService5Constructor() {
        Service5Constructor service = MockOsgi.activateInjectServices(Service5Constructor.class, bundleContext, map);

        assertNotNull(service);
        assertTrue(service.isActivated());

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService6() {
        Service6 service = new Service6();

        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());
        assertSame(bundleContext, service.getBundleContext());
        assertEquals("value1", service.getMap().get("prop1"));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService6Constructor() {
        Service6Constructor service = MockOsgi.activateInjectServices(Service6Constructor.class, bundleContext, map);

        assertNotNull(service);
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());
        assertSame(bundleContext, service.getBundleContext());
        assertEquals("value1", service.getMap().get("prop1"));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService7() {
        Service7 service = new Service7();

        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());
        assertSame(bundleContext, service.getBundleContext());
        assertEquals(map, Map.copyOf(service.getMap()));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService7Constructor() {
        Service7Constructor service = MockOsgi.activateInjectServices(Service7Constructor.class, bundleContext, map);

        assertNotNull(service);
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());
        assertSame(bundleContext, service.getBundleContext());
        assertEquals(map, Map.copyOf(service.getMap()));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testReferenceInConstructor() {
        try {
            MockOsgi.activateInjectServices(ServiceReferenceInConstructor.class, bundleContext);
            Assert.fail("Unresolvable mandatory reference in constructor should lead to ReferenceViolationException");
        } catch (ReferenceViolationException e) {
            String regex = "Unable to inject mandatory reference '.*' "
                    + Pattern.quote(
                            "(org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service1) into constructor parameter 0 for class org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.ServiceReferenceInConstructor : no matching services were found.");
            assertTrue(
                    "Expected exception message matching regex:\n" + regex + "\nbut got:\n" + e.getMessage(),
                    e.getMessage().matches(regex));
        }
    }

    /**
     * SLING-11860 verify OsgiServiceUtil#activateDeactivate invokes the correct activate and deactivate methods
     */
    @Test
    public void testService9ActivateDeactivate() {
        Service9 service = MockOsgi.activateInjectServices(Service9.class, bundleContext, map);
        assertEquals(Service9.class, service.getActivateFromClass());

        MockOsgi.deactivate(service, bundleContext, map);
        assertEquals(Service9.class, service.getDeactivateFromClass());
    }
}
