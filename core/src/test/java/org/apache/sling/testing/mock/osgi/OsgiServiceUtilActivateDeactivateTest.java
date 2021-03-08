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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service2;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service3;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service4;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service5;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service6;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service7;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.google.common.collect.ImmutableMap;

/**
 * Test different variants of activate/deactivate methods with varying signatures.
 */
public class OsgiServiceUtilActivateDeactivateTest {

    private Map<String,Object> map = ImmutableMap.<String, Object>of("prop1", "value1",
            "prop2.with.periods", "value2",
            "prop3-with-hyphens", "value3");
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
    public void testService2() {
        Service2 service = new Service2();

        assertTrue(MockOsgi.activate(service, bundleContext, "prop1", "value1"));
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
    public void testService4() {
        Service4 service = new Service4();

        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertEquals(map, ImmutableMap.copyOf(service.getMap()));

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
    public void testService7() {
        Service7 service = new Service7();

        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());
        assertSame(bundleContext, service.getBundleContext());
        assertEquals(map, ImmutableMap.copyOf(service.getMap()));

        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

}
