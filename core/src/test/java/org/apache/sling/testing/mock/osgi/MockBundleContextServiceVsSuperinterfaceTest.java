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
import java.util.Set;

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service7ServiceVsSuperInterface;
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

@RunWith(MockitoJUnitRunner.class)
public class MockBundleContextServiceVsSuperinterfaceTest {

    private BundleContext bundleContext;

    @Mock
    private ServiceInterface3 service1;

    @Mock
    private ServiceInterface3 service2;

    @Mock
    private ServiceSuperInterface3 superInterface1;

    @Mock
    private ServiceSuperInterface3 superInterface2;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
    }

    /**
     * Ensure that services that are injected have an exact match of the expected interface for the reference
     * and the interface the services are registered to - and not only assignable e.g. via super interface.
     */
    @Test
    public void testServiceRegistration() {
        ServiceRegistration<ServiceInterface3> regService1 =
                bundleContext.registerService(ServiceInterface3.class, service1, null);
        ServiceRegistration<ServiceSuperInterface3> regSuperInterface1 =
                bundleContext.registerService(ServiceSuperInterface3.class, superInterface1, null);

        Service7ServiceVsSuperInterface service = new Service7ServiceVsSuperInterface();
        MockOsgi.injectServices(service, bundleContext);
        MockOsgi.activate(service, bundleContext);
        bundleContext.registerService(Service7ServiceVsSuperInterface.class, service, null);

        assertDeps(service.getInterfaceDirectList(), service1);
        assertDeps(service.getInterfaceBindList(), service1);
        assertDeps(service.getSuperInterfaceDirectList(), superInterface1);
        assertDeps(service.getSuperInterfaceBindList(), superInterface1);

        bundleContext.registerService(ServiceInterface3.class, service2, null);
        bundleContext.registerService(ServiceSuperInterface3.class, superInterface2, null);

        assertDeps(service.getInterfaceDirectList(), service1, service2);
        assertDeps(service.getInterfaceBindList(), service1, service2);
        assertDeps(service.getSuperInterfaceDirectList(), superInterface1, superInterface2);
        assertDeps(service.getSuperInterfaceBindList(), superInterface1, superInterface2);

        regService1.unregister();
        regSuperInterface1.unregister();

        assertDeps(service.getInterfaceDirectList(), service2);
        assertDeps(service.getInterfaceBindList(), service2);
        assertDeps(service.getSuperInterfaceDirectList(), superInterface2);
        assertDeps(service.getSuperInterfaceBindList(), superInterface2);
    }

    @SafeVarargs
    private static void assertDeps(List<?> actual, Object... expected) {
        assertEquals(Set.of(expected), Set.copyOf(actual));
    }
}
