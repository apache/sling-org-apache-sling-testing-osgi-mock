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

import org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil.Service8;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MockBundleContextStaticGreedyCyclicTest {

    private BundleContext bundleContext;

    @Before
    public void setup() {
        bundleContext = MockOsgi.newBundleContext();

        Service8 impl = new Service8();
        bundleContext.registerService(Service8.class, impl, MapUtil.toDictionary("service.ranking", 10));
        assertNull(impl.getDefaultImpl());
    }

    @Test
    public void testCyclicReferenceFiltered() {
        DefaultService8 defaultImpl1 = new DefaultService8();
        ServiceRegistration defaultService1 = bundleContext.registerService(new String[] {
            Service8.class.getName(),
            DefaultService8.class.getName()
        }, defaultImpl1, null);

        Service8 impl = getService(Service8.class);
        assertEquals(defaultImpl1, impl.getDefaultImpl());

        // no default service expected
        defaultService1.unregister();
        impl = getService(Service8.class);
        assertNull(impl.getDefaultImpl());
    }

    private <T> T getService(Class<T> clazz) {
        ServiceReference<T> ref = bundleContext.getServiceReference(clazz);
        assertNotNull(ref);
        T srv = bundleContext.getService(ref);
        assertNotNull(srv);
        return srv;
    }

    private static class DefaultService8 extends Service8 {
        // no Reference to Service8
    }
}
