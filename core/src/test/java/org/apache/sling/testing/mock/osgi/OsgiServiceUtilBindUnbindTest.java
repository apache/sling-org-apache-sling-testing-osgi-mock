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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.ServiceInterface1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.activatedeactivate.Service9;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.bindunbind.Service1;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.bindunbind.Service2;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.bindunbind.Service3;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.bindunbind.Service4;
import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.bindunbind.Service5;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.assertEquals;

/**
 * Test different variants of bind/unbind methods with varying signatures.
 */
@RunWith(MockitoJUnitRunner.class)
public class OsgiServiceUtilBindUnbindTest {

    private BundleContext bundleContext;

    private ServiceRegistration<ServiceInterface1> reg1a;
    private ServiceRegistration<ServiceInterface1> reg1b;
    private Map<String, Object> props1a = MapUtil.toMap("prop1", "1a");
    private Map<String, Object> props1b = MapUtil.toMap("prop1", "1b");
    private Map<String, Object> props1c = MapUtil.toMap("prop1", "1c");

    @Mock
    private ServiceInterface1 instance1a;

    @Mock
    private ServiceInterface1 instance1b;

    @Mock
    private ServiceInterface1 instance1c;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
        reg1a = registerReference(instance1a, props1a);
        reg1b = registerReference(instance1b, props1b);
    }

    @Test
    public void testService1() {
        Service1 service = registerInjectService(new Service1());
        assertItems(service.getInstances(), instance1a, instance1b);

        registerReference(instance1c, props1c);
        assertItems(service.getInstances(), instance1a, instance1b, instance1c);

        reg1a.unregister();
        assertItems(service.getInstances(), instance1b, instance1c);
    }

    @Test
    public void testService2() {
        Service2 service = registerInjectService(new Service2());
        assertItems(service.getReferences(), reg1a.getReference(), reg1b.getReference());

        ServiceRegistration<ServiceInterface1> reg1c = registerReference(instance1c, props1c);
        assertItems(service.getReferences(), reg1a.getReference(), reg1b.getReference(), reg1c.getReference());

        reg1a.unregister();
        assertItems(service.getReferences(), reg1b.getReference(), reg1c.getReference());
    }

    @Test
    public void testService3() {
        Service3 service = registerInjectService(new Service3());
        assertItems(service.getReferences(), reg1a.getReference(), reg1b.getReference());
        assertItems(service.getInstances(), instance1a, instance1b);

        ServiceRegistration<ServiceInterface1> reg1c = registerReference(instance1c, props1c);
        assertItems(service.getInstances(), instance1a, instance1b, instance1c);
        assertItems(service.getReferences(), reg1a.getReference(), reg1b.getReference(), reg1c.getReference());

        reg1a.unregister();
        assertItems(service.getInstances(), instance1b, instance1c);
        assertItems(service.getReferences(), reg1b.getReference(), reg1c.getReference());
    }

    @Test
    public void testService4() {
        Service4 service = registerInjectService(new Service4());
        assertMaps(service.getConfigs(), props1a, props1b);

        registerReference(instance1c, props1c);
        assertMaps(service.getConfigs(), props1a, props1b, props1c);

        reg1a.unregister();
        assertMaps(service.getConfigs(), props1b, props1c);
    }

    @Test
    public void testService5() {
        Service5 service = registerInjectService(new Service5());
        assertItems(service.getReferences(), reg1a.getReference(), reg1b.getReference());
        assertItems(service.getInstances(), instance1a, instance1b);
        assertMaps(service.getConfigs(), props1a, props1b);

        ServiceRegistration<ServiceInterface1> reg1c = registerReference(instance1c, props1c);
        assertItems(service.getInstances(), instance1a, instance1b, instance1c);
        assertItems(service.getReferences(), reg1a.getReference(), reg1b.getReference(), reg1c.getReference());
        assertMaps(service.getConfigs(), props1a, props1b, props1c);

        reg1a.unregister();
        assertItems(service.getInstances(), instance1b, instance1c);
        assertItems(service.getReferences(), reg1b.getReference(), reg1c.getReference());
        assertMaps(service.getConfigs(), props1b, props1c);
    }

    @SuppressWarnings("null")
    private <T> T registerInjectService(T service) {
        MockOsgi.registerInjectActivateService(service, bundleContext);
        return service;
    }

    @SuppressWarnings("null")
    private <T extends ServiceInterface1> ServiceRegistration<ServiceInterface1> registerReference(
            T instance, Map<String, Object> props) {
        return bundleContext.registerService(ServiceInterface1.class, instance, MapUtil.toDictionary(props));
    }

    @SafeVarargs
    private final <T> void assertItems(List<T> actual, T... expected) {
        assertEquals(Set.of(expected), Set.copyOf(actual));
    }

    @SafeVarargs
    private final <T> void assertMaps(List<Map<String, Object>> actual, Map<String, Object>... expected) {
        List<Map<String, Object>> actualFiltered = actual.stream()
                .map(actualItem -> actualItem.entrySet().stream()
                        .filter(entry -> entry.getKey().equals("prop1"))
                        .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())))
                .collect(Collectors.toList());
        assertItems(actualFiltered, expected);
    }

    /**
     * SLING-11860 verify OsgiServiceUtil#invokeBindUnbindMethod invokes the correct bind and unbind methods
     */
    @Test
    public void testService9BindUnbind() {
        Service9 service9 = registerInjectService(new Service9());
        assertEquals(Service9.class, service9.getBindSvc1FromClass());

        reg1a.unregister();
        assertEquals(Service9.class, service9.getUnbindSvc1FromClass());
    }
}
